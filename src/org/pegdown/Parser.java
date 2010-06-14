/*
 * Copyright (C) 2010 Mathias Doenitz
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
import org.parboiled.ReportingParseRunner;
import org.parboiled.Rule;
import org.parboiled.annotations.Cached;
import org.parboiled.annotations.DontSkipActionsInPredicates;
import org.parboiled.annotations.SkipActionsInPredicates;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.common.ArrayBuilder;
import org.parboiled.common.StringUtils;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.StringVar;
import org.parboiled.support.Var;
import org.pegdown.ast.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parboiled parser for the standard and extended markdown syntax.
 * Builds an Abstract Syntax Tree (AST) of {@link Node} objects.
 */
@SuppressWarnings({"InfiniteRecursion"})
@SkipActionsInPredicates
public class Parser extends BaseParser<Node> implements SimpleNodeTypes, Extensions {

    static final String[] HTML_TAGS = new String[] {
            "address", "blockquote", "center", "dd", "dir", "div", "dl", "dt", "fieldset", "form", "frameset", "h1",
            "h2", "h3", "h4", "h5", "h6", "hr", "isindex", "li", "menu", "noframes", "noscript", "ol", "p", "pre",
            "script", "style", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "ul"
    };

    private final int options;

    final List<ReferenceNode> references = new ArrayList<ReferenceNode>();
    final List<AbbreviationNode> abbreviations = new ArrayList<AbbreviationNode>();

    public Parser(Integer options) {
        this.options = options;
    }

    //************* BLOCKS ****************

    @SuppressSubnodes // enable parboiled parse-tree-less parsing
    Rule Doc() {
        return Sequence(
                set(new Node()),
                ZeroOrMore(
                        Sequence(Block(), UP2(value().addChild(prevValue())))
                )
        );
    }

    Rule Block() {
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

    Rule Para() {
        return Sequence(
                NonindentSpace(), Inlines(), set(new ParaNode(prevValue())), OneOrMore(BlankLine())
        );
    }

    Rule BlockQuote() {
        StringVar innerSource = new StringVar("");
        return Sequence(
                OneOrMore(
                        Sequence(
                                '>', Optional(' '), Line(), innerSource.append(prevValue().getText()),
                                ZeroOrMore(
                                        Sequence(
                                                TestNot('>'),
                                                TestNot(BlankLine()),
                                                Line(), innerSource.append(prevValue().getText())
                                        )
                                ),
                                ZeroOrMore(
                                        Sequence(BlankLine(), innerSource.append("\n"))
                                )
                        )
                ),
                // trigger a recursive parsing run on the innerSource we just built
                // and attach the root of the inner parses AST
                set(new BlockQuoteNode(parseRawBlock(innerSource.get()).parseTreeRoot.getValue()))
        );
    }

    Rule Verbatim() {
        StringVar text = new StringVar("");
        StringVar temp = new StringVar("");
        return Sequence(
                OneOrMore(
                        Sequence(
                                ZeroOrMore(Sequence(BlankLine(), temp.append("\n"))),
                                NonblankIndentedLine(), text.append(temp.getAndSet(""), prevValue().getText())
                        )
                ),
                set(new VerbatimNode(text.get()))
        );
    }

    Rule HorizontalRule() {
        return Sequence(
                NonindentSpace(),
                FirstOf(HorizontalRule('*'), HorizontalRule('-'), HorizontalRule('_')),
                Sp(), Newline(), OneOrMore(BlankLine()),
                set(new SimpleNode(HRULE))
        );
    }

    Rule HorizontalRule(char c) {
        return Sequence(c, Sp(), c, Sp(), c, ZeroOrMore(Sequence(Sp(), c)));
    }

    //************* HEADINGS ****************

    Rule Heading() {
        return FirstOf(AtxHeading(), SetextHeading());
    }

    Rule AtxHeading() {
        return Sequence(
                AtxStart(), set(),
                Sp(),
                OneOrMore(
                        Sequence(AtxInline(), UP2(value().addChild(prevValue())))
                ),
                Optional(Sequence(Sp(), ZeroOrMore('#'), Sp())),
                Newline()
        );
    }

    Rule AtxStart() {
        return Sequence(
                FirstOf("######", "#####", "####", "###", "##", "#"),
                set(new HeaderNode(prevText().length()))
        );
    }

    Rule AtxInline() {
        return Sequence(TestNot(Newline()), TestNot(Sequence(Sp(), ZeroOrMore('#'), Sp(), Newline())), Inline());
    }

    Rule SetextHeading() {
        return FirstOf(SetextHeading1(), SetextHeading2());
    }

    Rule SetextHeading1() {
        return Sequence(
                SetextInline(), set(new HeaderNode(1, prevValue())),
                ZeroOrMore(
                        Sequence(SetextInline(), UP2(value().addChild(prevValue())))
                ),
                Newline(), NOrMore('=', 3), Newline()
        );
    }

    Rule SetextHeading2() {
        return Sequence(
                SetextInline(), set(new HeaderNode(2, prevValue())),
                ZeroOrMore(
                        Sequence(SetextInline(), UP2(value().addChild(prevValue())))
                ),
                Newline(), NOrMore('-', 3), Newline()
        );
    }

    Rule SetextInline() {
        return Sequence(TestNot(Endline()), Inline());
    }

    //************* LISTS ****************

    Rule BulletList() {
        return Sequence(
                Test(Bullet()),
                FirstOf(ListTight(), ListLoose()),
                set(new BulletListNode(prevValue()))
        );
    }

    Rule OrderedList() {
        return Sequence(
                Test(Enumerator()),
                FirstOf(ListTight(), ListLoose()),
                set(new OrderedListNode(prevValue()))
        );
    }

    Rule ListTight() {
        return Sequence(
                ListItem(true), set(new Node(prevValue())),
                ZeroOrMore(
                        Sequence(ListItem(true), UP2(value().addChild(prevValue())))
                ),
                ZeroOrMore(BlankLine()),
                TestNot(FirstOf(Bullet(), Enumerator()))
        );
    }

    Rule ListLoose() {
        return Sequence(
                ListItem(false), set(new Node(prevValue())),
                ZeroOrMore(BlankLine()),
                ZeroOrMore(
                        Sequence(ListItem(false), UP2(value().addChild(prevValue())), ZeroOrMore(BlankLine()))
                )
        );
    }

    Rule ListItem(boolean tight) {
        // for a simpler parser design we use a recursive parsing strategy for list items:
        // we collect the markdown source for an item, run a complete parsing cycle on this inner source and attach
        // the root of the inner parsing results AST to the outer AST tree

        StringVar innerSource = new StringVar();
        StringVar blanks = new StringVar("");
        StringVar extraNLs = new StringVar("");

        return Sequence(
                FirstOf(Bullet(), Enumerator()),

                ListBlock(),
                innerSource.set(prevValue().getText()) &&
                        (tight || extraNLs.set("\n\n")), // append extra \n\n to loose list items

                ZeroOrMore(
                        Sequence(
                                FirstOf(
                                        // if we have blank lines append them to the inner source
                                        OneOrMore(Sequence(BlankLine(), blanks.append("\n"))),

                                        // if we do not have a blank line we append a boundary marker
                                        blanks.set(tight ? "\u0001" : "\n\n\u0001")
                                ),
                                OneOrMore(
                                        Sequence(
                                                Indent(), ListBlock(),

                                                // append potentially captured blanks and the block text
                                                innerSource.append(blanks.getAndSet(""), prevValue().getText())
                                        )
                                ),
                                extraNLs.set("\n\n") // if we have several lines always add two extra newlines
                        )
                ),

                // finally, after having built the complete source we run an inner parse and attach its AST root
                setListItemNode(tight, innerSource.get() + extraNLs.get())
        );
    }

    // special action running the inner parse for list node source
    // the innerSource can contain \u0001 boundary markers, which indicate, where to split the innerSource
    // and run independent inner parsing runs
    boolean setListItemNode(boolean tight, String innerSource) {
        int start = 0;
        int end = innerSource.indexOf('\u0001', start); // look for boundary markers
        if (end == -1) {
            // if we have just one part simply parse and set
            Context<Node> context = getContext();
            Node innerRoot = parseRawBlock(innerSource).parseTreeRoot.getValue();
            setContext(context); // we need to save and restore the context since we might be recursing
            return set(tight ? new TightListItemNode(innerRoot) : new LooseListItemNode(innerRoot));
        }

        // ok, we have several parts, so create the root node and attach all part roots
        set(tight ? new TightListItemNode() : new LooseListItemNode());
        while (true) {
            end = innerSource.indexOf('\u0001', start);
            if (end == -1) end = innerSource.length();
            String sourcePart = innerSource.substring(start, end);

            Context<Node> context = getContext();
            Node node = parseRawBlock(sourcePart).parseTreeRoot.getValue();
            setContext(context);
            value().addChild(node.getChildren().get(0)); // skip one superfluous level

            if (end == innerSource.length()) return true;
            start = end + 1;
        }
    }

    Rule ListBlock() {
        StringVar source = new StringVar();
        return Sequence(
                Line(),
                source.set(prevValue().getText()),
                ZeroOrMore(
                        Sequence(ListBlockLine(), source.append(prevValue().getText()))
                ),
                set(new Node(source.get()))
        );
    }

    Rule ListBlockLine() {
        return Sequence(
                TestNot(Sequence(Optional(Indent()), FirstOf(Bullet(), Enumerator()))),
                TestNot(BlankLine()),
                TestNot(HorizontalRule()),
                OptionallyIndentedLine()
        );
    }

    Rule Enumerator() {
        return Sequence(NonindentSpace(), OneOrMore(Digit()), '.', OneOrMore(Spacechar()));
    }

    Rule Bullet() {
        return Sequence(TestNot(HorizontalRule()), NonindentSpace(), CharSet("+*-"), OneOrMore(Spacechar()));
    }

    //************* HTML BLOCK ****************

    Rule HtmlBlock() {
        return Sequence(
                FirstOf(HtmlBlockInTags(), HtmlComment(), HtmlBlockSelfClosing()),
                set(new HtmlBlockNode(prevText())),
                OneOrMore(BlankLine())
        );
    }

    Rule HtmlBlockInTags() {
        StringVar tagName = new StringVar();
        return Sequence(
                HtmlBlockOpen(), tagName.set(prevValue().getText()),
                ZeroOrMore(FirstOf(HtmlBlockInTags(), Sequence(TestNot(HtmlBlockClose(tagName)), Any()))),
                HtmlBlockClose(tagName)
        );
    }

    Rule HtmlBlockSelfClosing() {
        return Sequence('<', Spn1(), DefinedHtmlTagName(), Spn1(), ZeroOrMore(HtmlAttribute()), Optional('/'), Spn1(),
                '>');
    }

    Rule HtmlBlockOpen() {
        return Sequence('<', Spn1(), DefinedHtmlTagName(), set(), Spn1(), ZeroOrMore(HtmlAttribute()), '>');
    }

    @DontSkipActionsInPredicates
    Rule HtmlBlockClose(StringVar tagName) {
        return Sequence('<', Spn1(), '/', OneOrMore(Alphanumeric()), prevText().equals(tagName.get()), Spn1(), '>');
    }

    Rule DefinedHtmlTagName() {
        StringVar name = new StringVar();
        return Sequence(
                OneOrMore(Alphanumeric()),
                name.set(prevText().toLowerCase()) && // compare ignoring case
                        Arrays.binarySearch(HTML_TAGS, name.get()) >= 0 && // make sure its a defined tag
                        set(new Node(name.get()))
        );
    }

    //************* INLINES ****************

    Rule Inlines() {
        return Sequence(
                InlineOrIntermediateEndline(), set(new Node(prevValue())),
                ZeroOrMore(
                        Sequence(InlineOrIntermediateEndline(), UP2(value().addChild(prevValue())))
                ),
                Optional(Endline())
        );
    }

    Rule InlineOrIntermediateEndline() {
        return FirstOf(
                Sequence(TestNot(Endline()), Inline()),
                Sequence(Endline(), Test(Inline()))
        );
    }

    Rule Inline() {
        return FirstOf(new ArrayBuilder<Rule>()
                .add(Link(), Str(), Endline(), UlOrStarLine(), Space(), Strong(), Emph(), Image(), Code(), RawHtml(),
                        Entity(), EscapedChar())
                .addNonNulls(ext(QUOTES) ? new Rule[] {SingleQuoted(), DoubleQuoted()} : null)
                .addNonNulls(ext(SMARTS) ? new Rule[] {Ellipsis(), EnDash(), EmDash(), Apostrophe()} : null)
                .add(Symbol())
                .get()
        );
    }

    Rule Endline() {
        return FirstOf(LineBreak(), TerminalEndline(), NormalEndline());
    }

    Rule LineBreak() {
        return Sequence("  ", NormalEndline(), set(new SimpleNode(LINEBREAK)));
    }

    Rule TerminalEndline() {
        return Sequence(Sp(), Newline(), Eoi());
    }

    Rule NormalEndline() {
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
                ext(HARDWRAPS) ? ToRule(set(new SimpleNode(LINEBREAK))) : ToRule(set(new TextNode("\n")))
        );
    }

    //************* EMPHASIS / STRONG ****************

    // This keeps the parser from getting bogged down on long strings of '*' or '_',
    // or strings of '*' or '_' with space on each side:
    Rule UlOrStarLine() {
        return Sequence(
                FirstOf(CharLine('_'), CharLine('*')),
                set(new TextNode(prevText()))
        );
    }

    Rule CharLine(char c) {
        return FirstOf(NOrMore(c, 4), Sequence(Spacechar(), OneOrMore(c), Test(Spacechar())));
    }

    Rule Emph() {
        return Sequence(
                FirstOf(EmphOrStrong("*"), EmphOrStrong("_")),
                set(new EmphNode(prevValue()))
        );
    }

    Rule Strong() {
        return Sequence(
                FirstOf(EmphOrStrong("**"), EmphOrStrong("__")),
                set(new StrongNode(prevValue()))
        );
    }

    Rule EmphOrStrong(String chars) {
        return Sequence(
                EmphOrStrongOpen(chars),
                set(new Node()),
                ZeroOrMore(
                        Sequence(
                                TestNot(EmphOrStrongClose(chars)), TestNot(Newline()),
                                Inline(), UP2(value().addChild(prevValue()))
                        )
                ),
                EmphOrStrongClose(chars), value().addChild(prevValue())
        );
    }

    Rule EmphOrStrongOpen(String chars) {
        return Sequence(
                TestNot(CharLine(chars.charAt(0))),
                chars,
                TestNot(Spacechar()),
                TestNot(Newline())
        );
    }

    @Cached
    Rule EmphOrStrongClose(String chars) {
        return Sequence(
                TestNot(Spacechar()),
                TestNot(Newline()),
                Inline(), set(),
                chars.length() == 1 ? TestNot(EmphOrStrong(chars + chars)) : Empty(),
                chars,
                TestNot(Alphanumeric())
        );
    }

    //************* LINKS ****************

    Rule Image() {
        return Sequence('!',
                FirstOf(
                        Sequence(ExplicitLink(), set(((ExpLinkNode) prevValue()).asImage())),
                        Sequence(ReferenceLink(), set(((RefLinkNode) prevValue()).asImage()))
                )
        );
    }

    Rule Link() {
        return FirstOf(ExplicitLink(), ReferenceLink(), AutoLinkUrl(), AutoLinkEmail());
    }

    Rule ExplicitLink() {
        Var<ExpLinkNode> node = new Var<ExpLinkNode>();
        return Sequence(
                Label(), set(node.setAndGet(new ExpLinkNode(prevValue()))),
                Spn1(), '(', Sp(),
                Source(node),
                Spn1(), Optional(Title(node)),
                Sp(), ')'
        );
    }

    Rule ReferenceLink() {
        Var<RefLinkNode> node = new Var<RefLinkNode>();
        return Sequence(
                Label(), set(node.setAndGet(new RefLinkNode(prevValue()))),
                FirstOf(
                        // regular reference link
                        Sequence(Spn1(), node.get().setSeparatorSpace(prevText()),
                                Label(), node.get().setReferenceKey(prevValue())),

                        // implicit reference link
                        Sequence(Spn1(), node.get().setSeparatorSpace(prevText()), "[]"),

                        node.get().setSeparatorSpace(null) // implicit referencelink without trailing []
                )
        );
    }

    @Cached
    Rule Source(Var<ExpLinkNode> node) {
        StringVar url = new StringVar("");
        return FirstOf(
                Sequence('(', Source(node), ')'),
                Sequence('<', Source(node), '>'),
                Sequence(
                        OneOrMore(
                                FirstOf(
                                        Sequence('\\', CharSet("()"), url.append(prevChar())),
                                        Sequence(TestNot(CharSet("()>")), Nonspacechar(), url.append(prevChar()))
                                )
                        ),
                        node.get().setUrl(url.get())
                ),
                Empty()
        );
    }

    Rule Title(Var<ExpLinkNode> node) {
        return FirstOf(Title('\'', node), Title('"', node));
    }

    Rule Title(char delimiter, Var<ExpLinkNode> node) {
        return Sequence(
                delimiter,
                ZeroOrMore(
                        Sequence(TestNot(Sequence(delimiter, Sp(), FirstOf(')', Newline()))), TestNot(Newline()), Any())
                ),
                node.get().setTitle(prevText()),
                delimiter
        );
    }

    Rule AutoLinkUrl() {
        return Sequence(
                ext(AUTOLINKS) ? Optional('<') : Ch('<'),
                Sequence(OneOrMore(Letter()), "://", AutoLinkEnd()),
                set(new AutoLinkNode(prevText())),
                ext(AUTOLINKS) ? Optional('>') : Ch('>')
        );
    }

    Rule AutoLinkEmail() {
        return Sequence(
                ext(AUTOLINKS) ? Optional('<') : Ch('<'),
                Sequence(OneOrMore(FirstOf(Alphanumeric(), CharSet("-+_."))), '@', AutoLinkEnd()),
                set(new MailLinkNode(prevText())),
                ext(AUTOLINKS) ? Optional('>') : Ch('>')
        );
    }

    Rule AutoLinkEnd() {
        return OneOrMore(
                Sequence(
                        TestNot(Newline()),
                        ext(AUTOLINKS) ?
                                TestNot(Sequence(Optional(CharSet(".,;:)}]")), FirstOf(Spacechar(), Newline()))) :
                                TestNot('>'),
                        Any()
                )
        );
    }

    //************* REFERENCE ****************

    Rule Reference() {
        Var<ReferenceNode> ref = new Var<ReferenceNode>();
        return Sequence(
                NonindentSpace(), Label(), set(ref.setAndGet(new ReferenceNode(prevValue()))),
                ':', Sp(), RefSrc(ref),
                Spn1(), Optional(RefTitle(ref)),
                ZeroOrMore(BlankLine()),
                references.add(ref.get())
        );
    }

    Rule Label() {
        return Sequence(
                '[',
                set(new Node()),
                OneOrMore(Sequence(TestNot(']'), Inline(), UP2(value().addChild(prevValue())))),
                ']'
        );
    }

    Rule RefSrc(Var<ReferenceNode> ref) {
        return FirstOf(
                Sequence('<', RefSrcContent(ref), '>'),
                RefSrcContent(ref)
        );
    }

    Rule RefSrcContent(Var<ReferenceNode> ref) {
        return Sequence(OneOrMore(Sequence(TestNot('>'), Nonspacechar())), ref.get().setUrl(prevText()));
    }

    Rule RefTitle(Var<ReferenceNode> ref) {
        return FirstOf(RefTitle('\'', '\'', ref), RefTitle('"', '"', ref), RefTitle('(', ')', ref));
    }

    Rule RefTitle(char open, char close, Var<ReferenceNode> ref) {
        return Sequence(
                open,
                ZeroOrMore(
                        Sequence(TestNot(Sequence(close, Sp(), FirstOf(Newline(), Eoi()))), TestNot(Newline()), Any())
                ),
                ref.get().setTitle(prevText()),
                close
        );
    }

    //************* CODE ****************

    Rule Code() {
        return FirstOf(
                Code(Ticks(1)),
                Code(Ticks(2)),
                Code(Ticks(3)),
                Code(Ticks(4)),
                Code(Ticks(5))
        );
    }

    Rule Code(Rule ticks) {
        return Sequence(
                ticks, Sp(),
                OneOrMore(
                        FirstOf(
                                Sequence(TestNot('`'), Nonspacechar()),
                                Sequence(TestNot(ticks), OneOrMore('`')),
                                Sequence(TestNot(Sequence(Sp(), ticks)),
                                        FirstOf(Spacechar(), Sequence(Newline(), TestNot(BlankLine()))))
                        )
                ),
                set(new CodeNode(prevText())),
                Sp(), ticks
        );
    }

    Rule Ticks(int count) {
        return Sequence(StringUtils.repeat('`', count), TestNot('`'));
    }

    //************* RAW HTML ****************

    Rule RawHtml() {
        return Sequence(FirstOf(HtmlComment(), HtmlTag()), set(new TextNode(prevText())));
    }

    Rule HtmlComment() {
        return Sequence("<!--", ZeroOrMore(Sequence(TestNot("-->"), Any())), "-->");
    }

    Rule HtmlTag() {
        return Sequence('<', Spn1(), Optional('/'), OneOrMore(Alphanumeric()), Spn1(), ZeroOrMore(HtmlAttribute()),
                Optional('/'), Spn1(), '>');
    }

    Rule HtmlAttribute() {
        return Sequence(
                OneOrMore(FirstOf(Alphanumeric(), '-')),
                Spn1(),
                Optional(Sequence('=', Spn1(), FirstOf(Quoted(), OneOrMore(Sequence(TestNot('>'), Nonspacechar()))))),
                Spn1()
        );
    }

    Rule Quoted() {
        return FirstOf(
                Sequence('"', ZeroOrMore(Sequence(TestNot('"'), Any())), '"'),
                Sequence('\'', ZeroOrMore(Sequence(TestNot('\''), Any())), '\'')
        );
    }

    //************* LINES ****************

    Rule NonblankIndentedLine() {
        return Sequence(TestNot(BlankLine()), IndentedLine());
    }

    Rule BlankLine() {
        return Sequence(Sp(), Newline());
    }

    Rule IndentedLine() {
        return Sequence(Indent(), Line());
    }

    Rule OptionallyIndentedLine() {
        return Sequence(Optional(Indent()), Line());
    }

    Rule Line() {
        StringVar line = new StringVar();
        return Sequence(
                FirstOf(
                        Sequence(
                                ZeroOrMore(Sequence(TestNot('\r'), TestNot('\n'), Any())), line.set(prevText() + '\n'),
                                Newline()
                        ),
                        Sequence(OneOrMore(Any()), line.set(prevText()), Eoi())
                ),
                set(new Node(line.get()))
        );
    }

    //************* ENTITIES ****************

    Rule Entity() {
        return Sequence(FirstOf(HexEntity(), DecEntity(), CharEntity()), set(new TextNode(prevText())));
    }

    Rule HexEntity() {
        return Sequence("&", CharSet("xX"), OneOrMore(FirstOf(Digit(), CharRange('a', 'f'), CharRange('A', 'F'))), ';');
    }

    Rule DecEntity() {
        return Sequence("&#", OneOrMore(Digit()), ';');
    }

    Rule CharEntity() {
        return Sequence('&', OneOrMore(Alphanumeric()), ';');
    }

    //************* BASICS ****************

    Rule Str() {
        return Sequence(
                Sequence(NormalChar(), ZeroOrMore(Sequence(ZeroOrMore('_'), NormalChar()))),
                set(new TextNode(prevText()))
        );
    }

    Rule Space() {
        return Sequence(OneOrMore(Spacechar()), set(new TextNode(" ")));
    }

    Rule Spn1() {
        return Sequence(Sp(), Optional(Sequence(Newline(), Sp())));
    }

    Rule Sp() {
        return ZeroOrMore(Spacechar());
    }

    Rule Spacechar() {
        return CharSet(" \t");
    }

    Rule Nonspacechar() {
        return Sequence(TestNot(Spacechar()), TestNot(Newline()), Any());
    }

    Rule NormalChar() {
        return Sequence(TestNot(FirstOf(SpecialChar(), Spacechar(), Newline())), Any());
    }

    Rule EscapedChar() {
        return Sequence('\\', TestNot(Newline()), Any(), set(new TextNode(prevText())));
    }

    Rule Symbol() {
        return Sequence(SpecialChar(), set(new TextNode(prevText())));
    }

    Rule SpecialChar() {
        String chars = "*_`&[]<!\\";
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
        return CharSet(chars);
    }

    Rule Newline() {
        return FirstOf('\n', Sequence('\r', Optional('\n')));
    }

    Rule NonindentSpace() {
        return FirstOf("   ", "  ", " ", Empty());
    }

    Rule Indent() {
        return FirstOf('\t', "    ");
    }

    Rule Alphanumeric() {
        return FirstOf(Letter(), Digit());
    }

    Rule Letter() {
        return FirstOf(CharRange('A', 'Z'), CharRange('a', 'z'));
    }

    Rule Digit() {
        return CharRange('0', '9');
    }

    //************* ABBREVIATIONS ****************

    Rule Abbreviation() {
        Var<AbbreviationNode> node = new Var<AbbreviationNode>();
        return Sequence(
                NonindentSpace(), '*', Label(), set(node.setAndGet(new AbbreviationNode(prevValue()))),
                Sp(), ':', Sp(), AbbreviationText(node),
                ZeroOrMore(BlankLine()),
                abbreviations.add(node.get())
        );
    }

    Rule AbbreviationText(Var<AbbreviationNode> node) {
        return Sequence(
                node.get().setExpansion(new Node()),
                ZeroOrMore(Sequence(TestNot(Newline()), Inline(), node.get().getExpansion().addChild(prevValue())))
        );
    }

    //************* TABLES ****************

    Rule Table() {
        Var<TableNode> node = new Var<TableNode>();
        return Sequence(
                set(node.setAndGet(new TableNode())),
                ZeroOrMore(Sequence(TableRow(), node.get().addChild(((TableRowNode) prevValue()).asHeader()))),
                TableDivider(node),
                ZeroOrMore(Sequence(TableRow(), node.get().addChild(prevValue())))
        );
    }

    Rule TableDivider(Var<TableNode> tableNode) {
        Var<Boolean> pipeSeen = new Var<Boolean>(Boolean.FALSE);
        return Sequence(
                Optional(Sequence('|', pipeSeen.set(Boolean.TRUE))),
                OneOrMore(TableColumn(tableNode, pipeSeen)),
                pipeSeen.get() || tableNode.get().hasTwoOrMoreDividers(),
                Sp(), Newline()
        );
    }

    Rule TableColumn(Var<TableNode> tableNode, Var<Boolean> pipeSeen) {
        Var<TableColumnNode> node = new Var<TableColumnNode>(new TableColumnNode());
        return Sequence(
                Sp(),
                Optional(Sequence(':', node.get().markLeftAligned())),
                Sp(), OneOrMore('-'), Sp(),
                Optional(Sequence(':', node.get().markRightAligned())),
                Sp(),
                Optional(Sequence('|', pipeSeen.set(Boolean.TRUE))),
                tableNode.get().addColumn(node.get())
        );
    }

    Rule TableRow() {
        Var<Boolean> leadingPipe = new Var<Boolean>(Boolean.FALSE);
        return Sequence(
                set(new TableRowNode()),
                Optional(Sequence('|', leadingPipe.set(Boolean.TRUE))),
                OneOrMore(Sequence(TableCell(), UP2(value()).addChild(prevValue()))),
                leadingPipe.get() || value().getChildren().size() > 1 || prevText().endsWith("|"),
                Sp(), Newline()
        );
    }

    Rule TableCell() {
        return Sequence(
                set(new TableCellNode()),
                TestNot(Sequence(Sp(), Optional(':'), Sp(), OneOrMore('-'), Sp(), Optional(':'), Sp(),
                        FirstOf('|', Newline()))),
                Optional(Sequence(Sp(), TestNot('|'), TestNot(Newline()))),
                OneOrMore(Sequence(
                        TestNot('|'), TestNot(Sequence(Sp(), Newline())), Inline(),
                        UP2(value()).addChild(prevValue()),
                        Optional(Sequence(Sp(), Test('|'), Test(Newline())))
                )),
                ZeroOrMore('|'), ((TableCellNode) value()).setColSpan(Math.max(1, prevText().length()))
        );
    }

    //************* SMARTS ****************

    Rule Apostrophe() {
        return Sequence('\'', set(new SimpleNode(APOSTROPHE)));
    }

    Rule Ellipsis() {
        return Sequence(FirstOf("...", ". . ."), set(new SimpleNode(ELLIPSIS)));
    }

    Rule EnDash() {
        return Sequence('-', Test(Digit()), set(new SimpleNode(ENDASH)));
    }

    Rule EmDash() {
        return Sequence(FirstOf("---", "--"), set(new SimpleNode(EMDASH)));
    }

    //************* QUOTES ****************

    Rule SingleQuoted() {
        return Sequence(
                SingleQuoteStart(),
                set(new SinqleQuotedNode()),
                OneOrMore(Sequence(TestNot(SingleQuoteEnd()), Inline(), UP2(value().addChild(prevValue())))),
                SingleQuoteEnd()
        );
    }

    Rule SingleQuoteStart() {
        return Sequence(
                '\'',
                TestNot(CharSet(")!],.;:-? \t\n")),
                TestNot(
                        Sequence(
                                // do not convert the English apostrophes as in it's, I've, I'll, etc...
                                FirstOf('s', 't', "m", "ve", "ll", "re"),
                                TestNot(Alphanumeric())
                        )
                )
        );
    }

    Rule SingleQuoteEnd() {
        return Sequence('\'', TestNot(Alphanumeric()));
    }

    Rule DoubleQuoted() {
        return Sequence(
                '"',
                set(new DoubleQuotedNode()),
                OneOrMore(Sequence(TestNot('"'), Inline(), UP2(value().addChild(prevValue())))),
                '"'
        );
    }

    //************* HELPERS ****************

    boolean ext(int extension) {
        return (options & extension) > 0;
    }

    Rule NOrMore(char c, int n) {
        return Sequence(StringUtils.repeat(c, n), ZeroOrMore(c));
    }

    ParsingResult<Node> parseRawBlock(String text) {
        ParsingResult<Node> result = ReportingParseRunner.run(Doc(), text);
        if (!result.matched) {
            String errorMessage = "Internal error";
            if (result.hasErrors()) errorMessage += ": " + result.parseErrors.get(0);
            throw new RuntimeException(errorMessage);
        }
        return result;
    }

}
