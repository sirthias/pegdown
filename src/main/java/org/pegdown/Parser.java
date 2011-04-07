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
import org.parboiled.common.StringUtils;
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

/**
 * Parboiled parser for the standard and extended markdown syntax.
 * Builds an Abstract Syntax Tree (AST) of {@link Node} objects.
 */
@SuppressWarnings( {"InfiniteRecursion"})
@SkipActionsInPredicates
public class Parser extends BaseParser<Object> implements Extensions {

    public interface ParseRunnerProvider {
        ParseRunner<Node> get(Rule rule);
    }

    private final int options;
    private final ParseRunnerProvider parseRunnerProvider;
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
                        .addNonNulls(ext(TABLES) ? Table() : null)
                        .add(Reference(), HorizontalRule(), Heading(), OrderedList(), BulletList(), HtmlBlock(), Para(),
                                Inlines())
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
                        '>', Optional(' '), Line(), inner.append(popAsTextNode().getText()),
                        ZeroOrMore(
                                TestNot('>'),
                                TestNot(BlankLine()),
                                Line(), inner.append(popAsTextNode().getText())
                        ),
                        ZeroOrMore(BlankLine(), inner.append("\n"))
                ),
                // trigger a recursive parsing run on the inner source we just built
                // and attach the root of the inner parses AST
                push(new BlockQuoteNode(parseInternal(inner.getChars())))
        );
    }

    public Rule Verbatim() {
        StringBuilderVar text = new StringBuilderVar();
        StringBuilderVar temp = new StringBuilderVar();
        return NodeSequence(
                OneOrMore(
                        ZeroOrMore(BlankLine(), temp.append("\n")),
                        NonblankIndentedLine(),
                        text.appended(temp.getString()).append(popAsTextNode().getText()) && temp.clearContents()
                ),
                push(new VerbatimNode(text.getString()))
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
                // test for successful setext heading before actually building it to save unnecessary node building
                Test(OneOrMore(TestNot(Newline()), ANY), Newline(), FirstOf(NOrMore('=', 3), NOrMore('-', 3)),
                        Newline()),
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

    //************* LISTS ****************

    public Rule BulletList() {
        return NodeSequence(
                Test(Bullet()),
                FirstOf(ListTight(), ListLoose()),
                push(new BulletListNode(popAsNode().getChildren()))
        );
    }

    public Rule OrderedList() {
        return NodeSequence(
                Test(Enumerator()),
                FirstOf(ListTight(), ListLoose()),
                push(new OrderedListNode(popAsNode().getChildren()))
        );
    }

    public Rule ListTight() {
        return Sequence(
                ListItem(true), push(new SuperNode(popAsNode())),
                ZeroOrMore(ListItem(true), addAsChild()),
                ZeroOrMore(BlankLine()),
                TestNot(FirstOf(Bullet(), Enumerator()))
        );
    }

    public Rule ListLoose() {
        return Sequence(
                ListItem(false), push(new SuperNode(popAsNode())),
                ZeroOrMore(BlankLine()),
                ZeroOrMore(ListItem(false), addAsChild(), ZeroOrMore(BlankLine()))
        );
    }

    @Cached
    public Rule ListItem(boolean tight) {
        // for a simpler parser design we use a recursive parsing strategy for list items:
        // we collect the markdown source for an item, run a complete parsing cycle on this inner source and attach
        // the root of the inner parsing results AST to the outer AST tree

        StringBuilderVar inner = new StringBuilderVar();
        StringVar blanks = new StringVar("");
        StringVar extraNLs = new StringVar("");

        return Sequence(
                FirstOf(Bullet(), Enumerator()),

                ListBlock(),
                inner.set(new StringBuilder(popAsTextNode().getText())) &&
                        (tight || extraNLs.set("\n\n")), // append extra \n\n to loose list items

                ZeroOrMore(
                        FirstOf(
                                // if we have blank lines append them to the inner source
                                OneOrMore(BlankLine(), blanks.append("\n")),

                                // if we do not have a blank line we append a boundary marker
                                blanks.set(tight ? "\u0001" : "\n\n\u0001")
                        ),
                        OneOrMore(
                                Indent(), ListBlock(),

                                // append potentially captured blanks and the block text
                                inner.appended(blanks.getAndSet("")).append(popAsTextNode().getText())
                        ),
                        extraNLs.set("\n\n") // if we have several lines always add two extra newlines
                ),

                // finally, after having built the complete source we run an inner parse and attach its AST root
                setListItemNode(tight, inner.appended(extraNLs.get()).getChars())
        );
    }

    // special action running the inner parse for list node source
    // the innerSource can contain \u0001 boundary markers, which indicate, where to split the innerSource
    // and run independent inner parsings

    boolean setListItemNode(boolean tight, char[] innerSource) {
        SuperNode parent = tight ? new TightListItemNode() : new LooseListItemNode();
        int start = 0;
        int end = indexOf(innerSource, '\u0001', start); // look for boundary markers
        if (end == -1) {
            // if we have just one part simply parse and set
            Context<Object> context = getContext();
            Node innerRoot = parseInternal(innerSource);
            setContext(context); // we need to save and restore the context since we might be recursing
            parent = tight ? new TightListItemNode(innerRoot) : new LooseListItemNode(innerRoot); 
        } else {
            // ok, we have several parts, so create the root node and attach all part roots
            while (true) {
                end = indexOf(innerSource, '\u0001', start);
                if (end == -1) end = innerSource.length;
                char[] sourcePart = new char[end - start];
                System.arraycopy(innerSource, start, sourcePart, 0, end - start);
    
                Context<Object> context = getContext();
                SuperNode node = parseInternal(sourcePart);
                setContext(context);
                parent.getChildren().add(node); // skip one superfluous level
    
                if (end == innerSource.length) break;
                start = end + 1;
            }
        }
        return push(parent);
    }

    public Rule ListBlock() {
        StringBuilderVar source = new StringBuilderVar();
        return Sequence(
                Line(),
                source.set(new StringBuilder(popAsTextNode().getText())),
                ZeroOrMore(ListBlockLine(), source.append(popAsTextNode().getText())),
                push(new TextNode(source.getString()))
        );
    }

    public Rule ListBlockLine() {
        return Sequence(
                TestNot(Optional(Indent()), FirstOf(Bullet(), Enumerator())),
                TestNot(BlankLine()),
                TestNot(HorizontalRule()),
                OptionallyIndentedLine()
        );
    }

    public Rule Enumerator() {
        return Sequence(NonindentSpace(), OneOrMore(Digit()), '.', OneOrMore(Spacechar()));
    }

    public Rule Bullet() {
        return Sequence(TestNot(HorizontalRule()), NonindentSpace(), AnyOf("+*-"), OneOrMore(Spacechar()));
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

    private static final String[] HTML_TAGS = new String[] {
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
        return FirstOf(new ArrayBuilder<Rule>()
                .add(Link(), Str(), Endline(), UlOrStarLine(), Space(), Strong(), Emph(), Image(), Code(), InlineHtml(),
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
        return NodeSequence(Sp(), Newline(), EOI, push(new TextNode("\n")));
    }

    public Rule NormalEndline() {
        return Sequence(
                Sp(), Newline(),
                TestNot(
                        FirstOf(
                                BlankLine(),
                                '>',
                                AtxStart(),
                                Sequence(Line(), FirstOf(NOrMore('=', 3), NOrMore('-', 3)), Newline())
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
                OneOrMore(
                        TestNot(EmphOrStrongClose(chars)), TestNot(Newline()),
                        Inline(), addAsChild()
                ),
                EmphOrStrongClose(chars)
        );
    }

    public Rule EmphOrStrongOpen(String chars) {
        return Sequence(
                TestNot(CharLine(chars.charAt(0))),
                chars,
                TestNot(Spacechar()),
                TestNot(Newline())
        );
    }

    @Cached
    public Rule EmphOrStrongClose(String chars) {
        return Sequence(
                TestNot(Spacechar()),
                TestNot(Newline()),
                chars.length() == 1 ? TestNot(EmphOrStrong(chars + chars)) : EMPTY,
                chars,
                TestNot(Alphanumeric())
        );
    }

    //************* LINKS ****************

    public Rule Image() {
        return NodeSequence('!',
                FirstOf(
                        Sequence(ExplicitLink(), push(((ExpLinkNode) pop()).asImage())),
                        Sequence(ReferenceLink(), push(((RefLinkNode) pop()).asImage()))
                )
        );
    }

    @MemoMismatches
    public Rule Link() {
        return NodeSequence(FirstOf(ExplicitLink(), ReferenceLink(), AutoLinkUrl(), AutoLinkEmail()));
    }

    public Rule ExplicitLink() {
        Var<ExpLinkNode> node = new Var<ExpLinkNode>();
        return Sequence(
                Label(), push(node.setAndGet(new ExpLinkNode(popAsNode()))),
                Spn1(), '(', Sp(),
                Source(node),
                Spn1(), Optional(Title(node)),
                Sp(), ')'
        );
    }

    public Rule ReferenceLink() {
        Var<RefLinkNode> node = new Var<RefLinkNode>();
        return Sequence(
                Label(), push(node.setAndGet(new RefLinkNode(popAsNode()))),
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
                ZeroOrMore(TestNot(delimiter, Sp(), FirstOf(')', Newline())), TestNot(Newline()), ANY),
                node.get().setTitle(match()),
                delimiter
        );
    }

    public Rule AutoLinkUrl() {
        return Sequence(
                ext(AUTOLINKS) ? Optional('<') : Ch('<'),
                Sequence(OneOrMore(Letter()), "://", AutoLinkEnd()),
                push(new AutoLinkNode(match())),
                ext(AUTOLINKS) ? Optional('>') : Ch('>')
        );
    }

    public Rule AutoLinkEmail() {
        return Sequence(
                ext(AUTOLINKS) ? Optional('<') : Ch('<'),
                Sequence(OneOrMore(FirstOf(Alphanumeric(), AnyOf("-+_."))), '@', AutoLinkEnd()),
                push(new MailLinkNode(match())),
                ext(AUTOLINKS) ? Optional('>') : Ch('>')
        );
    }

    public Rule AutoLinkEnd() {
        return OneOrMore(
                TestNot(Newline()),
                ext(AUTOLINKS) ?
                        TestNot(
                                FirstOf(
                                        '>',
                                        Sequence(Optional(AnyOf(".,;:)}]")), FirstOf(Spacechar(), Newline()))
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
                OneOrMore(TestNot(']'), Inline(), addAsChild()),
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
                ZeroOrMore(TestNot(close, Sp(), Newline()), TestNot(Newline()), ANY),
                ref.get().setTitle(match()),
                close
        );
    }

    //************* CODE ****************

    public Rule Code() {
        return NodeSequence(
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
        return Sequence(StringUtils.repeat('`', count), TestNot('`'));
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

    public Rule NonblankIndentedLine() {
        return Sequence(TestNot(BlankLine()), IndentedLine());
    }

    public Rule BlankLine() {
        return Sequence(Sp(), Newline());
    }

    public Rule IndentedLine() {
        return Sequence(Indent(), Line());
    }

    public Rule OptionallyIndentedLine() {
        return Sequence(Optional(Indent()), Line());
    }

    public Rule Line() {
        return Sequence(
                ZeroOrMore(TestNot('\r'), TestNot('\n'), ANY), push(match()),
                Newline(),
                push(new TextNode(((String) pop()) + '\n'))
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
        return Sequence(TestNot(Spacechar()), TestNot(Newline()), ANY);
    }

    @MemoMismatches
    public Rule NormalChar() {
        return Sequence(TestNot(FirstOf(SpecialChar(), Spacechar(), Newline())), ANY);
    }

    public Rule EscapedChar() {
        return NodeSequence('\\', TestNot(Newline()), ANY, push(new SpecialTextNode(match())));
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
        if (ext(TABLES)) {
            chars += '|';
        }
        return AnyOf(chars);
    }

    @MemoMismatches
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
                    ZeroOrMore(TestNot(Newline()), Inline(), addAsChild())
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
                Optional(Sp(), TestNot('|'), TestNot(Newline())),
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
                SingleQuoteStart(),
                push(new QuotedNode(QuotedNode.Type.Single)),
                OneOrMore(TestNot(SingleQuoteEnd()), Inline(), addAsChild()),
                SingleQuoteEnd()
        );
    }

    public Rule SingleQuoteStart() {
        return Sequence(
                '\'',
                TestNot(AnyOf(")!],.;:-? \t\n")),
                TestNot(
                        // do not convert the English apostrophes as in it's, I've, I'll, etc...
                        FirstOf('s', 't', "m", "ve", "ll", "re"),
                        TestNot(Alphanumeric())
                )
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
        return Sequence(StringUtils.repeat(c, n), ZeroOrMore(c));
    }
    
    public Rule NodeSequence(Object... nodeRules) {
        return Sequence(
                push(getContext().getCurrentIndex()),
                Sequence(nodeRules),
                setIndices()
        );
    }
    
    boolean setIndices() {
        AbstractNode node = (AbstractNode) peek();
        node.setStartIndex((Integer)pop(1));
        node.setEndIndex(getContext().getCurrentIndex());
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

    public RootNode parseInternal(char[] source) {
        ParsingResult<Node> result = parseInternal2(source);
        if (result.hasErrors()) {
            throw new RuntimeException("Internal error during markdown parsing:\n--- ParseErrors ---\n" +
                    printParseErrors(result)/* +
                    "\n--- ParseTree ---\n" +
                    printNodeTree(result)*/
            );
        }
        return (RootNode) result.resultValue;
    }
    
    ParsingResult<Node> parseInternal2(char[] source) {
        return parseRunnerProvider.get(Root()).run(source);
    } 

    public int indexOf(char[] array, char element, int start) {
        for (int i = start; i < array.length; i++) {
            if (array[i] == element) return i;
        }
        return -1;
    }
}
