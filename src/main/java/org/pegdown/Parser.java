/*
 * Copyright (C) 2010-2011 Mathias Doenitz
 *
 * Based on peg-markdown (C) 2008-2010 John MacFarlane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pegdown;

import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.*;
import org.parboiled.common.ArrayBuilder;
import org.parboiled.common.ImmutableList;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.StringBuilderVar;
import org.parboiled.support.StringVar;
import org.parboiled.support.Var;
import org.pegdown.ast.*;
import org.pegdown.ast.SimpleNode.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.parboiled.errors.ErrorUtils.printParseErrors;
import static org.parboiled.common.StringUtils.repeat;

/**
 * Parboiled parser for the standard and extended markdown syntax.
 * Builds an Abstract Syntax Tree (AST) of {@link Node} objects.
 */
@SuppressWarnings( {"InfiniteRecursion"})
public class Parser extends BaseParser<Object> implements Extensions {
    
    protected static final char CROSSED_OUT = '\uffff';

    public interface ParseRunnerProvider {
        ParseRunner<Node> get(Rule rule);
    }

    protected final int options;
    protected final ParseRunnerProvider parseRunnerProvider;
    final List<AbbreviationNode> abbreviations = new ArrayList<AbbreviationNode>();
    final List<ReferenceNode> references = new ArrayList<ReferenceNode>();

    public Parser(Integer options) {
        this(options, new Parser.ParseRunnerProvider() {
            public ParseRunner<Node> get(Rule rule) {
                return new ReportingParseRunner<Node>(rule);
            }
        });
    }

    public Parser(Integer options, ParseRunnerProvider parseRunnerProvider) {
        this.options = options;
        this.parseRunnerProvider = parseRunnerProvider;
    }

    public RootNode parse(char[] source) {
        try {
            RootNode root = parseInternal(source);
            root.setAbbreviations(ImmutableList.copyOf(abbreviations));
            root.setReferences(ImmutableList.copyOf(references));
            return root;
        } finally {
            abbreviations.clear();
            references.clear();
        }
    }

    //************* BLOCKS ****************

    public Rule Root() {
        return NodeSequence(
                push(new RootNode()),
                ZeroOrMore(Block(), addAsChild())
        );
    }

    public Rule Block() {
        return Sequence(
                ZeroOrMore(BlankLine()),
                FirstOf(new ArrayBuilder<Rule>()
                        .add(BlockQuote(), Verbatim())
                        .addNonNulls(ext(ABBREVIATIONS) ? Abbreviation() : null)
                        .add(Reference(), HorizontalRule(), Heading(), OrderedList(), BulletList(), HtmlBlock())
                        .addNonNulls(ext(TABLES) ? Table() : null)
                        .addNonNulls(ext(DEFINITIONS) ? DefinitionList() : null)
                        .addNonNulls(ext(FENCED_CODE_BLOCKS) ? FencedCodeBlock() : null)
                        .add(Para(), Inlines())
                        .get()
                )
        );
    }

    public Rule Para() {
        return NodeSequence(
                NonindentSpace(), Inlines(), push(new ParaNode(popAsNode())), OneOrMore(BlankLine())
        );
    }

    public Rule BlockQuote() {
        StringBuilderVar inner = new StringBuilderVar();
        return NodeSequence(
                OneOrMore(
                        CrossedOut(Sequence('>', Optional(' ')), inner), Line(inner),
                        ZeroOrMore(
                                TestNot('>'),
                                TestNot(BlankLine()),
                                Line(inner)
                        ),
                        ZeroOrMore(BlankLine(), inner.append(match()))
                ),
                // trigger a recursive parsing run on the inner source we just built
                // and attach the root of the inner parses AST
                push(new BlockQuoteNode(withIndicesShifted(parseInternal(inner), (Integer)peek()).getChildren()))
        );
    }

    public Rule Verbatim() {
        StringBuilderVar text = new StringBuilderVar();
        StringBuilderVar line = new StringBuilderVar();
        return NodeSequence(
                OneOrMore(
                        ZeroOrMore(BlankLine(), line.append("\n")),
                        Indent(), push(currentIndex()), 
                        OneOrMore(
                                FirstOf(
                                        Sequence('\t', line.append(repeat(' ', 4-(currentIndex()-1-(Integer)peek())%4))),
                                        Sequence(NotNewline(), ANY, line.append(matchedChar()))
                                )
                        ),
                        Newline(),
                        text.appended(line.getString()).append('\n') && line.clearContents() && drop()
                ),
                push(new VerbatimNode(text.getString()))
        );
    }
    
    public Rule FencedCodeBlock() {
        StringBuilderVar text = new StringBuilderVar();
        Var<Integer> markerLength = new Var<Integer>();
        return NodeSequence(
                CodeFence(markerLength),
                TestNot(CodeFence(markerLength)), // prevent empty matches
                ZeroOrMore(BlankLine(), text.append('\n')),
                OneOrMore(TestNot(Newline(), CodeFence(markerLength)), ANY, text.append(matchedChar())),
                Newline(),
                push(new VerbatimNode(text.appended('\n').getString())),
                CodeFence(markerLength)
        );
    }
    
    @Cached
    public Rule CodeFence(Var<Integer> markerLength) {
        return Sequence(
                FirstOf(NOrMore('~', 3), NOrMore('`', 3)),
                (markerLength.isSet() && matchLength() == markerLength.get()) ||
                        (markerLength.isNotSet() && markerLength.set(matchLength())),
                Sp(),
                ZeroOrMore(TestNot(Newline()), ANY), // GFM code type identifier
                Newline()
        );
    }
    
    public Rule HorizontalRule() {
        return NodeSequence(
                NonindentSpace(),
                FirstOf(HorizontalRule('*'), HorizontalRule('-'), HorizontalRule('_')),
                Sp(), Newline(), OneOrMore(BlankLine()),
                push(new SimpleNode(Type.HRule))
        );
    }

    public Rule HorizontalRule(char c) {
        return Sequence(c, Sp(), c, Sp(), c, ZeroOrMore(Sp(), c));
    }

    //************* HEADINGS ****************

    public Rule Heading() {
        return NodeSequence(FirstOf(AtxHeading(), SetextHeading()));
    }

    public Rule AtxHeading() {
        return Sequence(
                AtxStart(),
                Optional(Sp()),
                OneOrMore(AtxInline(), addAsChild()),
                Optional(Sp(), ZeroOrMore('#'), Sp()),
                Newline()
        );
    }

    public Rule AtxStart() {
        return Sequence(
                FirstOf("######", "#####", "####", "###", "##", "#"),
                push(new HeaderNode(match().length()))
        );
    }

    public Rule AtxInline() {
        return Sequence(
                TestNot(Newline()),
                TestNot(Optional(Sp()), ZeroOrMore('#'), Sp(), Newline()),
                Inline()
        );
    }

    public Rule SetextHeading() {
        return Sequence(
                // test for successful setext heading before actually building it to reduce backtracking
                Test(OneOrMore(NotNewline(), ANY), Newline(), FirstOf(NOrMore('=', 3), NOrMore('-', 3)), Newline()),
                FirstOf(SetextHeading1(), SetextHeading2())
        );
    }

    public Rule SetextHeading1() {
        return Sequence(
                SetextInline(), push(new HeaderNode(1, popAsNode())),
                ZeroOrMore(SetextInline(), addAsChild()),
                Newline(), NOrMore('=', 3), Newline()
        );
    }

    public Rule SetextHeading2() {
        return Sequence(
                SetextInline(), push(new HeaderNode(2, popAsNode())),
                ZeroOrMore(SetextInline(), addAsChild()),
                Newline(), NOrMore('-', 3), Newline()
        );
    }

    public Rule SetextInline() {
        return Sequence(TestNot(Endline()), Inline());
    }

    //************** Definition Lists ************
    
    public Rule DefinitionList() {
        return NodeSequence(
                // test for successful definition list match before actually building it to reduce backtracking
                TestNot(Spacechar()),
                Test(
                        OneOrMore(TestNot(BlankLine()), TestNot(DefListBullet()),
                                OneOrMore(NotNewline(), ANY), Newline()),
                        Optional(BlankLine()),
                        DefListBullet()
                ),
                push(new DefinitionListNode()),
                OneOrMore(
                        push(new SuperNode()),
                        OneOrMore(DefListTerm(), addAsChild()),
                        OneOrMore(Definition(), addAsChild()),
                        ((SuperNode)peek(1)).getChildren().addAll(popAsNode().getChildren()),
                        Optional(BlankLine())
                )
        );
    }
    
    public Rule DefListTerm() {
        return NodeSequence(
                TestNot(Spacechar()),
                TestNot(DefListBullet()),
                push(new DefinitionTermNode()),
                OneOrMore(DefTermInline(), addAsChild()),
                Optional(':'),
                Newline()
        );
    }
    
    public Rule DefTermInline() {
        return Sequence(
                NotNewline(),
                TestNot(':', Newline()),
                Inline()
        );
    }
    
    public Rule Definition() {
        SuperNodeCreator itemNodeCreator = new SuperNodeCreator() {
            public SuperNode create(Node child) {
                return new DefinitionNode(child);
            }
        };
        return ListItem(DefListBullet(), itemNodeCreator);
    }
    
    public Rule DefListBullet() {
        return Sequence(NonindentSpace(), AnyOf(":~"), OneOrMore(Spacechar()));
    }

    //************* LISTS ****************

    public Rule BulletList() {
        SuperNodeCreator itemNodeCreator = new SuperNodeCreator() {
            public SuperNode create(Node child) {
                return new ListItemNode(child);
            }
        };
        return NodeSequence(
                ListItem(Bullet(), itemNodeCreator), push(new BulletListNode(popAsNode())),
                ZeroOrMore(ListItem(Bullet(), itemNodeCreator), addAsChild()),
                ZeroOrMore(BlankLine())
        );
    }

    public Rule OrderedList() {
        SuperNodeCreator itemNodeCreator = new SuperNodeCreator() {
            public SuperNode create(Node child) {
                return new ListItemNode(child);
            }
        };
        return NodeSequence(
                ListItem(Enumerator(), itemNodeCreator), push(new OrderedListNode(popAsNode())),
                ZeroOrMore(ListItem(Enumerator(), itemNodeCreator), addAsChild()),
                ZeroOrMore(BlankLine())
        );
    }

    @Cached
    public Rule ListItem(Rule itemStart, SuperNodeCreator itemNodeCreator) {
        // for a simpler parser design we use a recursive parsing strategy for list items:
        // we collect a number of markdown source blocks for an item, run complete parsing cycle on these and attach
        // the roots of the inner parsing results AST to the outer AST tree
        StringBuilderVar block = new StringBuilderVar();
        StringBuilderVar temp = new StringBuilderVar();
        Var<Boolean> tight = new Var<Boolean>(false);
        Var<SuperNode> tightFirstItem = new Var<SuperNode>();
        return Sequence(
                push(getContext().getCurrentIndex()),
                FirstOf(CrossedOut(BlankLine(), block), tight.set(true)),
                CrossedOut(itemStart, block), Line(block),
                ZeroOrMore(
                        Optional(CrossedOut(Indent(), temp)),
                        NotItem(),
                        Line(temp),
                        block.append(temp.getString()) && temp.clearContents()
                ),
                tight.get() ? push(tightFirstItem.setAndGet(itemNodeCreator.create(parseListBlock(block)))) :
                        fixFirstItem((SuperNode) peek(1)) &&
                                push(itemNodeCreator.create(parseListBlock(block.appended("\n\n")))),
                ZeroOrMore(
                        push(getContext().getCurrentIndex()),
                        FirstOf(Sequence(CrossedOut(BlankLine(), block), tight.set(false)), tight.set(true)),
                        CrossedOut(Indent(), block),
                        FirstOf(
                                DoubleIndentedBlocks(block),
                                IndentedBlock(block)
                        ),
                        (tight.get() ? push(parseListBlock(block)) :
                                (tightFirstItem.isNotSet() || wrapFirstItemInPara(tightFirstItem.get())) &&
                                        push(parseListBlock(block.appended("\n\n")))
                        ) && addAsChild()
                ),
                setListItemIndices()
        );
    }
    
    public Rule CrossedOut(Rule rule, StringBuilderVar block) {
        return Sequence(rule, appendCrossed(block));
    }
    
    public Rule DoubleIndentedBlocks(StringBuilderVar block) {
        StringBuilderVar line = new StringBuilderVar();
        return Sequence(
                Indent(), TestNot(BlankLine()), block.append("    "), Line(block),
                ZeroOrMore(
                        ZeroOrMore(BlankLine(), line.append(match())),
                        CrossedOut(Indent(), line), Indent(), line.append("    "), Line(line),
                        block.append(line.getString()) && line.clearContents()
                )
        );
    }
    
    public Rule IndentedBlock(StringBuilderVar block) {
        return Sequence(
                Line(block),
                ZeroOrMore(
                    FirstOf(
                            Sequence(TestNot(BlankLine()), CrossedOut(Indent(), block)),
                            NotItem()
                    ),
                    Line(block)
                )
        );
    }
    
    public Rule NotItem() {
        return TestNot(
                FirstOf(new ArrayBuilder<Rule>()
                        .add(Bullet(), Enumerator(), BlankLine(), HorizontalRule())
                        .addNonNulls(ext(DEFINITIONS) ? DefListBullet() : null)
                        .get()
                )
        );
    }
    
    public Rule Enumerator() {
        return Sequence(NonindentSpace(), OneOrMore(Digit()), '.', OneOrMore(Spacechar()));
    }

    public Rule Bullet() {
        return Sequence(TestNot(HorizontalRule()), NonindentSpace(), AnyOf("+*-"), OneOrMore(Spacechar()));
    }
    
    //************* LIST ITEM ACTIONS ****************

    boolean appendCrossed(StringBuilderVar block) {
        for (int i = 0; i < matchLength(); i++) {
            block.append(CROSSED_OUT);
        }
        return true;
    }

    Node parseListBlock(StringBuilderVar block) {
        Context<Object> context = getContext();
        Node innerRoot = parseInternal(block);
        setContext(context); // we need to save and restore the context since we might be recursing
        block.clearContents();
        return withIndicesShifted(innerRoot, (Integer) context.getValueStack().pop());
    }
    
    Node withIndicesShifted(Node node, int delta) {
        ((AbstractNode) node).shiftIndices(delta);
        for (Node subNode : node.getChildren()) {
            withIndicesShifted(subNode, delta);
        }
        return node;
    }

    boolean fixFirstItem(SuperNode listNode) {
        List<Node> items = listNode.getChildren();
        if (items.size() == 1 && items.get(0) instanceof ListItemNode) {
            wrapFirstItemInPara((SuperNode) items.get(0));
        }
        return true;
    }

    boolean wrapFirstItemInPara(SuperNode item) {
        Node firstItemFirstChild = item.getChildren().get(0);
        ParaNode paraNode = new ParaNode(firstItemFirstChild.getChildren());
        paraNode.setStartIndex(firstItemFirstChild.getStartIndex());
        paraNode.setEndIndex(firstItemFirstChild.getEndIndex());
        item.getChildren().set(0, paraNode);
        return true;
    }
    
    boolean setListItemIndices() {
        SuperNode listItem = (SuperNode) getContext().getValueStack().peek();
        List<Node> children = listItem.getChildren();
        listItem.setStartIndex(children.get(0).getStartIndex());
        listItem.setEndIndex(children.get(children.size()-1).getEndIndex());
        return true;
    }

    //************* HTML BLOCK ****************

    public Rule HtmlBlock() {
        return NodeSequence(
                FirstOf(HtmlBlockInTags(), HtmlComment(), HtmlBlockSelfClosing()),
                push(new HtmlBlockNode(ext(SUPPRESS_HTML_BLOCKS) ? "" : match())),
                OneOrMore(BlankLine())
        );
    }

    public Rule HtmlBlockInTags() {
        StringVar tagName = new StringVar();
        return Sequence(
                Test(HtmlBlockOpen(tagName)), // get the type of tag if there is one
                HtmlTagBlock(tagName) // specifically match that type of tag
        );
    }

    @Cached
    public Rule HtmlTagBlock(StringVar tagName) {
        return Sequence(
                HtmlBlockOpen(tagName),
                ZeroOrMore(
                        FirstOf(
                                HtmlTagBlock(tagName),
                                Sequence(TestNot(HtmlBlockClose(tagName)), ANY)
                        )
                ),
                HtmlBlockClose(tagName)
        );
    }

    public Rule HtmlBlockSelfClosing() {
        StringVar tagName = new StringVar();
        return Sequence('<', Spn1(), DefinedHtmlTagName(tagName), Spn1(), ZeroOrMore(HtmlAttribute()), Optional('/'),
                Spn1(), '>');
    }

    public Rule HtmlBlockOpen(StringVar tagName) {
        return Sequence('<', Spn1(), DefinedHtmlTagName(tagName), Spn1(), ZeroOrMore(HtmlAttribute()), '>');
    }

    @DontSkipActionsInPredicates
    public Rule HtmlBlockClose(StringVar tagName) {
        return Sequence('<', Spn1(), '/', OneOrMore(Alphanumeric()), match().equals(tagName.get()), Spn1(), '>');
    }

    @Cached
    public Rule DefinedHtmlTagName(StringVar tagName) {
        return Sequence(
                OneOrMore(Alphanumeric()),
                tagName.isSet() && match().equals(tagName.get()) ||
                        tagName.isNotSet() && tagName.set(match().toLowerCase()) && isHtmlTag(tagName.get())
        );
    }

    public boolean isHtmlTag(String string) {
        return Arrays.binarySearch(HTML_TAGS, string) >= 0;
    }

    protected static final String[] HTML_TAGS = new String[] {
            "address", "blockquote", "center", "dd", "dir", "div", "dl", "dt", "fieldset", "form", "frameset", "h1",
            "h2", "h3", "h4", "h5", "h6", "hr", "isindex", "li", "menu", "noframes", "noscript", "ol", "p", "pre",
            "script", "style", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "ul"
    };

    //************* INLINES ****************

    public Rule Inlines() {
        return NodeSequence(
                InlineOrIntermediateEndline(), push(new SuperNode(popAsNode())),
                ZeroOrMore(InlineOrIntermediateEndline(), addAsChild()),
                Optional(Endline(), drop())
        );
    }

    public Rule InlineOrIntermediateEndline() {
        return FirstOf(
                Sequence(TestNot(Endline()), Inline()),
                Sequence(Endline(), Test(Inline()))
        );
    }

    @MemoMismatches
    public Rule Inline() {
        return FirstOf(Link(), NonLinkInline());
    }

    public Rule NonAutoLinkInline() {
        return FirstOf(NonAutoLink(), NonLinkInline());
    }

    public Rule NonLinkInline() {
        return FirstOf(new ArrayBuilder<Rule>()
                .add(Str(), Endline(), UlOrStarLine(), Space(), StrongOrEmph(), Image(), Code(), InlineHtml(),
                        Entity(), EscapedChar())
                .addNonNulls(ext(QUOTES) ? new Rule[] {SingleQuoted(), DoubleQuoted(), DoubleAngleQuoted()} : null)
                .addNonNulls(ext(SMARTS) ? new Rule[] {Smarts()} : null)
                .add(Symbol())
                .get()
        );
    }

    @MemoMismatches
    public Rule Endline() {
        return NodeSequence(FirstOf(LineBreak(), TerminalEndline(), NormalEndline()));
    }

    public Rule LineBreak() {
        return Sequence("  ", NormalEndline(), poke(new SimpleNode(Type.Linebreak)));
    }

    public Rule TerminalEndline() {
        return NodeSequence(Sp(), Newline(), Test(EOI), push(new TextNode("\n")));
    }

    public Rule NormalEndline() {
        return Sequence(
                Sp(), Newline(),
                TestNot(
                        FirstOf(
                                BlankLine(),
                                '>',
                                AtxStart(),
                                Sequence(ZeroOrMore(NotNewline(), ANY), Newline(),
                                        FirstOf(NOrMore('=', 3), NOrMore('-', 3)), Newline())
                        )
                ),
                ext(HARDWRAPS) ? toRule(push(new SimpleNode(Type.Linebreak))) : toRule(push(new TextNode(" ")))
        );
    }

    //************* EMPHASIS / STRONG ****************

    @MemoMismatches
    public Rule UlOrStarLine() {
        // This keeps the parser from getting bogged down on long strings of '*' or '_',
        // or strings of '*' or '_' with space on each side:
        return NodeSequence(
                FirstOf(CharLine('_'), CharLine('*')),
                push(new TextNode(match()))
        );
    }

    public Rule CharLine(char c) {
        return FirstOf(NOrMore(c, 4), Sequence(Spacechar(), OneOrMore(c), Test(Spacechar())));
    }
    
    public Rule StrongOrEmph() {
        return Sequence(
                Test(AnyOf("*_")),
                FirstOf(Strong(), Emph())
        );
    }

    public Rule Emph() {
        return NodeSequence(
                FirstOf(EmphOrStrong("*"), EmphOrStrong("_")),
                push(new EmphNode(popAsNode().getChildren()))
        );
    }

    public Rule Strong() {
        return NodeSequence(
                FirstOf(EmphOrStrong("**"), EmphOrStrong("__")),
                push(new StrongNode(popAsNode().getChildren()))
        );
    }

    @Cached
    public Rule EmphOrStrong(String chars) {
        return Sequence(
                EmphOrStrongOpen(chars),
                push(new SuperNode()),
                OneOrMore(TestNot(EmphOrStrongClose(chars)), NotNewline(), Inline(), addAsChild()),
                EmphOrStrongClose(chars)
        );
    }

    public Rule EmphOrStrongOpen(String chars) {
        return Sequence(
                TestNot(CharLine(chars.charAt(0))),
                chars,
                TestNot(Spacechar()),
                NotNewline()
        );
    }

    @Cached
    public Rule EmphOrStrongClose(String chars) {
        return Sequence(
                TestNot(Spacechar()),
                NotNewline(),
                chars.length() == 1 ? TestNot(EmphOrStrong(chars + chars)) : EMPTY,
                chars,
                TestNot(Alphanumeric())
        );
    }

    //************* LINKS ****************

    public Rule Image() {
        return NodeSequence(
                '!', Label(),
                FirstOf(
                        Sequence(ExplicitLink(), ((ExpLinkNode) peek()).makeImage()),
                        Sequence(ReferenceLink(), ((RefLinkNode) peek()).makeImage())
                )
        );
    }

    @MemoMismatches
    public Rule Link() {
        return NodeSequence(
                FirstOf(
                        ext(WIKILINKS) ? WikiLink() : NOTHING,
                        Sequence(Label(), FirstOf(ExplicitLink(), ReferenceLink())),
                        AutoLink()
                )
        );
    }

    public Rule NonAutoLink() {
        return NodeSequence(Sequence(Label(), FirstOf(ExplicitLink(), ReferenceLink())));
    }

    public Rule ExplicitLink() {
        Var<ExpLinkNode> node = new Var<ExpLinkNode>();
        return Sequence(
                push(node.setAndGet(new ExpLinkNode(popAsNode(), ext(NO_FOLLOW_LINKS)))),
                Spn1(), '(', Sp(),
                Source(node),
                Spn1(), Optional(Title(node)),
                Sp(), ')'
        );
    }

    public Rule ReferenceLink() {
        Var<RefLinkNode> node = new Var<RefLinkNode>();
        return Sequence(
                push(node.setAndGet(new RefLinkNode(popAsNode(), ext(NO_FOLLOW_LINKS)))),
                FirstOf(
                        // regular reference link
                        Sequence(Spn1(), node.get().setSeparatorSpace(match()),
                                Label(), node.get().setReferenceKey((SuperNode) pop())),

                        // implicit reference link
                        Sequence(Spn1(), node.get().setSeparatorSpace(match()), "[]"),

                        node.get().setSeparatorSpace(null) // implicit referencelink without trailing []
                )
        );
    }

    @Cached
    public Rule Source(Var<ExpLinkNode> node) {
        StringBuilderVar url = new StringBuilderVar();
        return FirstOf(
                Sequence('(', Source(node), ')'),
                Sequence('<', Source(node), '>'),
                Sequence(
                        OneOrMore(
                                FirstOf(
                                        Sequence('\\', AnyOf("()"), url.append(matchedChar())),
                                        Sequence(TestNot(AnyOf("()>")), Nonspacechar(), url.append(matchedChar()))
                                )
                        ),
                        node.get().setUrl(url.getString())
                ),
                EMPTY
        );
    }

    public Rule Title(Var<ExpLinkNode> node) {
        return FirstOf(Title('\'', node), Title('"', node));
    }

    public Rule Title(char delimiter, Var<ExpLinkNode> node) {
        return Sequence(
                delimiter,
                ZeroOrMore(TestNot(delimiter, Sp(), FirstOf(')', Newline())), NotNewline(), ANY),
                node.get().setTitle(match()),
                delimiter
        );
    }

    public Rule AutoLink() {
        return Sequence(
                ext(AUTOLINKS) ? Optional('<') : Ch('<'),
                FirstOf(AutoLinkUrl(), AutoLinkEmail()),
                ext(AUTOLINKS) ? Optional('>') : Ch('>')
        );
    }

    public Rule WikiLink() {
        return Sequence(
            "[[",
            OneOrMore(TestNot(']'), ANY), // Not sure ANY is appropriate here, but certainly some symbols and whitespace are
            push(new WikiLinkNode(match(), ext(NO_FOLLOW_LINKS))),
            "]]"
        );
    }

    public Rule AutoLinkUrl() {
        return Sequence(
                Sequence(OneOrMore(Letter()), "://", AutoLinkEnd()),
                push(new AutoLinkNode(match(), ext(NO_FOLLOW_LINKS)))
        );
    }

    public Rule AutoLinkEmail() {
        return Sequence(
                Sequence(OneOrMore(FirstOf(Alphanumeric(), AnyOf("-+_."))), '@', AutoLinkEnd()),
                push(new MailLinkNode(match()))
        );
    }

    public Rule AutoLinkEnd() {
        return OneOrMore(
                TestNot(Newline()),
                ext(AUTOLINKS) ?
                        TestNot(
                                FirstOf(
                                        '>',
                                        Sequence(Optional(AnyOf(".,;:)}]\"'")), FirstOf(Spacechar(), Newline()))
                                )
                        ) :
                        TestNot('>'),
                ANY
        );
    }

    //************* REFERENCE ****************

    public Rule Label() {
        return Sequence(
                '[',
                push(new SuperNode()),
                OneOrMore(TestNot(']'), NonAutoLinkInline(), addAsChild()),
                ']'
        );
    }
    
    public Rule Reference() {
        Var<ReferenceNode> ref = new Var<ReferenceNode>();
        return NodeSequence(
                NonindentSpace(), Label(), push(ref.setAndGet(new ReferenceNode(popAsNode()))),
                ':', Spn1(), RefSrc(ref),
                Sp(), Optional(RefTitle(ref)),
                Sp(), Newline(),
                ZeroOrMore(BlankLine()),
                references.add(ref.get())
        );
    }

    public Rule RefSrc(Var<ReferenceNode> ref) {
        return FirstOf(
                Sequence('<', RefSrcContent(ref), '>'),
                RefSrcContent(ref)
        );
    }

    public Rule RefSrcContent(Var<ReferenceNode> ref) {
        return Sequence(OneOrMore(TestNot('>'), Nonspacechar()), ref.get().setUrl(match()));
    }

    public Rule RefTitle(Var<ReferenceNode> ref) {
        return FirstOf(RefTitle('\'', '\'', ref), RefTitle('"', '"', ref), RefTitle('(', ')', ref));
    }

    public Rule RefTitle(char open, char close, Var<ReferenceNode> ref) {
        return Sequence(
                open,
                ZeroOrMore(TestNot(close, Sp(), Newline()), NotNewline(), ANY),
                ref.get().setTitle(match()),
                close
        );
    }

    //************* CODE ****************

    public Rule Code() {
        return NodeSequence(
                Test('`'),
                FirstOf(
                        Code(Ticks(1)),
                        Code(Ticks(2)),
                        Code(Ticks(3)),
                        Code(Ticks(4)),
                        Code(Ticks(5))
                )
        );
    }

    public Rule Code(Rule ticks) {
        return Sequence(
                ticks, Sp(),
                OneOrMore(
                        FirstOf(
                                Sequence(TestNot('`'), Nonspacechar()),
                                Sequence(TestNot(ticks), OneOrMore('`')),
                                Sequence(TestNot(Sp(), ticks),
                                        FirstOf(Spacechar(), Sequence(Newline(), TestNot(BlankLine()))))
                        )
                ),
                push(new CodeNode(match())),
                Sp(), ticks
        );
    }

    public Rule Ticks(int count) {
        return Sequence(repeat('`', count), TestNot('`'));
    }

    //************* RAW HTML ****************

    public Rule InlineHtml() {
        return NodeSequence(
                FirstOf(HtmlComment(), HtmlTag()),
                push(new InlineHtmlNode(ext(SUPPRESS_INLINE_HTML) ? "" : match()))
        );
    }

    public Rule HtmlComment() {
        return Sequence("<!--", ZeroOrMore(TestNot("-->"), ANY), "-->");
    }

    public Rule HtmlTag() {
        return Sequence('<', Spn1(), Optional('/'), OneOrMore(Alphanumeric()), Spn1(), ZeroOrMore(HtmlAttribute()),
                Optional('/'), Spn1(), '>');
    }

    public Rule HtmlAttribute() {
        return Sequence(
                OneOrMore(FirstOf(Alphanumeric(), '-')),
                Spn1(),
                Optional('=', Spn1(), FirstOf(Quoted(), OneOrMore(TestNot('>'), Nonspacechar()))),
                Spn1()
        );
    }

    public Rule Quoted() {
        return FirstOf(
                Sequence('"', ZeroOrMore(TestNot('"'), ANY), '"'),
                Sequence('\'', ZeroOrMore(TestNot('\''), ANY), '\'')
        );
    }

    //************* LINES ****************

    public Rule BlankLine() {
        return Sequence(Sp(), Newline());
    }

    public Rule Line(StringBuilderVar sb) {
        return Sequence(
                Sequence(ZeroOrMore(NotNewline(), ANY), Newline()),
                sb.append(match())
        );
    }

    //************* ENTITIES ****************

    public Rule Entity() {
        return NodeSequence(
                Sequence('&', FirstOf(HexEntity(), DecEntity(), CharEntity()), ';'),
                push(new TextNode(match()))
        );
    }

    public Rule HexEntity() {
        return Sequence('#', IgnoreCase('x'), OneOrMore(FirstOf(Digit(), CharRange('a', 'f'), CharRange('A', 'F'))));
    }

    public Rule DecEntity() {
        return Sequence('#', OneOrMore(Digit()));
    }

    public Rule CharEntity() {
        return OneOrMore(Alphanumeric());
    }

    //************* BASICS ****************

    public Rule Str() {
        return NodeSequence(OneOrMore(NormalChar()), push(new TextNode(match())));
    }

    public Rule Space() {
        return NodeSequence(OneOrMore(Spacechar()), push(new TextNode(" ")));
    }

    public Rule Spn1() {
        return Sequence(Sp(), Optional(Newline(), Sp()));
    }

    public Rule Sp() {
        return ZeroOrMore(Spacechar());
    }

    public Rule Spacechar() {
        return AnyOf(" \t");
    }

    public Rule Nonspacechar() {
        return Sequence(TestNot(Spacechar()), NotNewline(), ANY);
    }

    @MemoMismatches
    public Rule NormalChar() {
        return Sequence(TestNot(SpecialChar()), TestNot(Spacechar()), NotNewline(), ANY);
    }

    public Rule EscapedChar() {
        return NodeSequence('\\', NotNewline(), ANY, push(new SpecialTextNode(match())));
    }

    public Rule Symbol() {
        return NodeSequence(SpecialChar(), push(new SpecialTextNode(match())));
    }

    public Rule SpecialChar() {
        String chars = "*_`&[]<>!#\\";
        if (ext(QUOTES)) {
            chars += "'\"";
        }
        if (ext(SMARTS)) {
            chars += ".-";
        }
        if (ext(AUTOLINKS)) {
            chars += "(){}";
        }
        if (ext(DEFINITIONS)) {
            chars += ":";
        }
        if (ext(TABLES)) {
            chars += "|";
        }
        if (ext(DEFINITIONS) | ext(FENCED_CODE_BLOCKS)) {
            chars += "~";
        }
        return AnyOf(chars);
    }

    public Rule NotNewline() {
        return TestNot(AnyOf("\n\r"));
    }
    
    public Rule Newline() {
        return FirstOf('\n', Sequence('\r', Optional('\n')));
    }

    public Rule NonindentSpace() {
        return FirstOf("   ", "  ", " ", EMPTY);
    }

    public Rule Indent() {
        return FirstOf('\t', "    ");
    }

    public Rule Alphanumeric() {
        return FirstOf(Letter(), Digit());
    }

    public Rule Letter() {
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'));
    }

    public Rule Digit() {
        return CharRange('0', '9');
    }

    //************* ABBREVIATIONS ****************

    public Rule Abbreviation() {
        Var<AbbreviationNode> node = new Var<AbbreviationNode>();
        return NodeSequence(
                NonindentSpace(), '*', Label(), push(node.setAndGet(new AbbreviationNode(popAsNode()))),
                Sp(), ':', Sp(), AbbreviationText(node),
                ZeroOrMore(BlankLine()),
                abbreviations.add(node.get())
        );
    }

    public Rule AbbreviationText(Var<AbbreviationNode> node) {
        return Sequence(
                NodeSequence(
                        push(new SuperNode()),
                        ZeroOrMore(NotNewline(), Inline(), addAsChild())
                ),
                node.get().setExpansion(popAsNode())
        );
    }

    //************* TABLES ****************

    public Rule Table() {
        Var<TableNode> node = new Var<TableNode>();
        return NodeSequence(
                push(node.setAndGet(new TableNode())),
                Optional(
                        NodeSequence(
                                TableRow(), push(1, new TableHeaderNode()) && addAsChild(),
                                ZeroOrMore(TableRow(), addAsChild())
                        ),
                        addAsChild() // add the TableHeaderNode to the TableNode
                ),
                TableDivider(node),
                Optional(
                        NodeSequence(
                                TableRow(), push(1, new TableBodyNode()) && addAsChild(),
                                ZeroOrMore(TableRow(), addAsChild())
                        ),
                        addAsChild() // add the TableHeaderNode to the TableNode
                ),
                !node.get().getChildren().isEmpty()
                // only accept as table if we have at least one header or at least one body
        );
    }

    public Rule TableDivider(Var<TableNode> tableNode) {
        Var<Boolean> pipeSeen = new Var<Boolean>(Boolean.FALSE);
        return Sequence(
                Optional('|', pipeSeen.set(Boolean.TRUE)),
                OneOrMore(TableColumn(tableNode, pipeSeen)),
                pipeSeen.get() || tableNode.get().hasTwoOrMoreDividers(),
                Sp(), Newline()
        );
    }

    public Rule TableColumn(Var<TableNode> tableNode, Var<Boolean> pipeSeen) {
        Var<TableColumnNode> node = new Var<TableColumnNode>(new TableColumnNode());
        return Sequence(
                Sp(),
                Optional(':', node.get().markLeftAligned()),
                Sp(), OneOrMore('-'), Sp(),
                Optional(':', node.get().markRightAligned()),
                Sp(),
                Optional('|', pipeSeen.set(Boolean.TRUE)),
                tableNode.get().addColumn(node.get())
        );
    }

    public Rule TableRow() {
        Var<Boolean> leadingPipe = new Var<Boolean>(Boolean.FALSE);
        return NodeSequence(
                push(new TableRowNode()),
                Optional('|', leadingPipe.set(Boolean.TRUE)),
                OneOrMore(TableCell(), addAsChild()),
                leadingPipe.get() || ((Node) peek()).getChildren().size() > 1 ||
                        getContext().getInputBuffer().charAt(matchEnd() - 1) == '|',
                Sp(), Newline()
        );
    }

    public Rule TableCell() {
        return NodeSequence(
                push(new TableCellNode()),
                TestNot(Sp(), Optional(':'), Sp(), OneOrMore('-'), Sp(), Optional(':'), Sp(), FirstOf('|', Newline())),
                Optional(Sp(), TestNot('|'), NotNewline()),
                OneOrMore(
                        TestNot('|'), TestNot(Sp(), Newline()), Inline(),
                        addAsChild(),
                        Optional(Sp(), Test('|'), Test(Newline()))
                ),
                ZeroOrMore('|'), ((TableCellNode) peek()).setColSpan(Math.max(1, matchLength()))
        );
    }

    //************* SMARTS ****************

    public Rule Smarts() {
        return NodeSequence(
                FirstOf(
                        Sequence(FirstOf("...", ". . ."), push(new SimpleNode(Type.Ellipsis))),
                        Sequence("---", push(new SimpleNode(Type.Emdash))),
                        Sequence("--", push(new SimpleNode(Type.Endash))),
                        Sequence('\'', push(new SimpleNode(Type.Apostrophe)))
                )
        );
    }

    //************* QUOTES ****************

    public Rule SingleQuoted() {
        return NodeSequence(
                !Character.isLetter(getContext().getInputBuffer().charAt(getContext().getCurrentIndex() - 1)),
                '\'',
                push(new QuotedNode(QuotedNode.Type.Single)),
                OneOrMore(TestNot(SingleQuoteEnd()), Inline(), addAsChild()),
                SingleQuoteEnd()
        );
    }

    public Rule SingleQuoteEnd() {
        return Sequence('\'', TestNot(Alphanumeric()));
    }

    public Rule DoubleQuoted() {
        return NodeSequence(
                '"',
                push(new QuotedNode(QuotedNode.Type.Double)),
                OneOrMore(TestNot('"'), Inline(), addAsChild()),
                '"'
        );
    }

    public Rule DoubleAngleQuoted() {
        return NodeSequence(
                "<<",
                push(new QuotedNode(QuotedNode.Type.DoubleAngle)),
                Optional(NodeSequence(Spacechar(), push(new SimpleNode(Type.Nbsp))), addAsChild()),
                OneOrMore(
                        FirstOf(
                                Sequence(NodeSequence(OneOrMore(Spacechar()), Test(">>"),
                                        push(new SimpleNode(Type.Nbsp))), addAsChild()),
                                Sequence(TestNot(">>"), Inline(), addAsChild())
                        )
                ),
                ">>"
        );
    }

    //************* HELPERS ****************
    
    public Rule NOrMore(char c, int n) {
        return Sequence(repeat(c, n), ZeroOrMore(c));
    }
    
    public Rule NodeSequence(Object... nodeRules) {
        return Sequence(
                push(getContext().getCurrentIndex()),
                Sequence(nodeRules),
                setIndices()
        );
    }
    
    public boolean setIndices() {
        AbstractNode node = (AbstractNode) peek();
        node.setStartIndex((Integer)pop(1));
        node.setEndIndex(currentIndex());
        return true;
    }
    
    public boolean addAsChild() {
        SuperNode parent = (SuperNode) peek(1);
        List<Node> children = parent.getChildren();
        Node child = popAsNode();
        if (child.getClass() == TextNode.class && !children.isEmpty()) {
            Node lastChild = children.get(children.size() - 1);
            if (lastChild.getClass() == TextNode.class) {
                // collapse peer TextNodes
                TextNode last = (TextNode) lastChild;
                TextNode current = (TextNode) child;
                last.append(current.getText());
                last.setEndIndex(current.getEndIndex());
                return true;
            }
        }
        children.add(child);
        return true;
    }
    
    public Node popAsNode() {
        return (Node) pop();
    }

    public TextNode popAsTextNode() {
        return (TextNode) pop();
    }

    public boolean ext(int extension) {
        return (options & extension) > 0;
    }
    
    // called for inner parses for list items and blockquotes
    public RootNode parseInternal(StringBuilderVar block) {
        char[] chars = block.getChars();
        int[] ixMap = new int[chars.length + 1]; // map of cleaned indices to original indices
        
        // strip out CROSSED_OUT characters and build index map
        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c != CROSSED_OUT) {
                ixMap[clean.length()] = i;
                clean.append(c);
            }
        }
        ixMap[clean.length()] = chars.length;
        
        // run inner parse
        char[] cleaned = new char[clean.length()];
        clean.getChars(0, cleaned.length, cleaned, 0);
        RootNode rootNode = parseInternal(cleaned);
        
        // correct AST indices with index map
        fixIndices(rootNode, ixMap);
        
        return rootNode;
    }

    protected void fixIndices(Node node, int[] ixMap) {
        ((AbstractNode) node).mapIndices(ixMap);
        for (Node subNode : node.getChildren()) {
            fixIndices(subNode, ixMap);
        }
    }

    public RootNode parseInternal(char[] source) {
        ParsingResult<Node> result = parseToParsingResult(source);
        if (result.hasErrors()) {
            throw new RuntimeException("Internal error during markdown parsing:\n--- ParseErrors ---\n" +
                    printParseErrors(result)/* +
                    "\n--- ParseTree ---\n" +
                    printNodeTree(result)*/
            );
        }
        return (RootNode) result.resultValue;
    }
    
    ParsingResult<Node> parseToParsingResult(char[] source) {
        return parseRunnerProvider.get(Root()).run(source);
    }

    protected interface SuperNodeCreator {
        SuperNode create(Node child);
    }
}
