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
import org.parboiled.Rule;
import org.parboiled.annotations.*;
import org.parboiled.common.ArrayBuilder;
import org.parboiled.common.Factory;
import org.parboiled.common.StringUtils;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
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
@BuildParseTree
public class Parser extends BaseParser<Node> implements SimpleNodeTypes, Extensions {

    static final String[] HTML_TAGS = new String[] {
            "address", "blockquote", "center", "dd", "dir", "div", "dl", "dt", "fieldset", "form", "frameset", "h1",
            "h2", "h3", "h4", "h5", "h6", "hr", "isindex", "li", "menu", "noframes", "noscript", "ol", "p", "pre",
            "script", "style", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "ul"
    };

    private final int options;
    public Factory<ParseRunner<Node>> parseRunnerFactory = new Factory<ParseRunner<Node>>() {
        public ParseRunner<Node> create() {
            return new ReportingParseRunner<Node>(Doc());
        }
    };

    final List<ReferenceNode> references = new ArrayList<ReferenceNode>();
    final List<AbbreviationNode> abbreviations = new ArrayList<AbbreviationNode>();

    public Parser(Integer options) {
        this.options = options;
    }

    //************* BLOCKS ****************

    Rule Doc() {
        return Sequence(
                push(new Node()),
                ZeroOrMore(Block(), peek(1).addChild(pop()))
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
                NonindentSpace(), Inlines(), push(new ParaNode(pop())), OneOrMore(BlankLine())
        );
    }

    Rule BlockQuote() {
        StringVar innerSource = new StringVar("");
        return Sequence(
                OneOrMore(
                        '>', Optional(' '), Line(), innerSource.append(pop().getText()),
                        ZeroOrMore(
                                TestNot('>'),
                                TestNot(BlankLine()),
                                Line(), innerSource.append(pop().getText())
                        ),
                        ZeroOrMore(BlankLine(), innerSource.append("\n"))
                ),
                // trigger a recursive parsing run on the innerSource we just built
                // and attach the root of the inner parses AST
                push(new BlockQuoteNode(parseRawBlock(innerSource.get()).resultValue))
        );
    }

    Rule Verbatim() {
        StringVar text = new StringVar("");
        StringVar temp = new StringVar("");
        return Sequence(
                OneOrMore(
                        ZeroOrMore(BlankLine(), temp.append("\n")),
                        NonblankIndentedLine(), text.append(temp.getAndSet(""), pop().getText())
                ),
                push(new VerbatimNode(text.get()))
        );
    }

    Rule HorizontalRule() {
        return Sequence(
                NonindentSpace(),
                FirstOf(HorizontalRule('*'), HorizontalRule('-'), HorizontalRule('_')),
                Sp(), Newline(), OneOrMore(BlankLine()),
                push(new SimpleNode(HRULE))
        );
    }

    Rule HorizontalRule(char c) {
        return Sequence(c, Sp(), c, Sp(), c, ZeroOrMore(Sp(), c));
    }

    //************* HEADINGS ****************

    Rule Heading() {
        return FirstOf(AtxHeading(), SetextHeading());
    }

    Rule AtxHeading() {
        return Sequence(
                AtxStart(),
                Sp(),
                OneOrMore(AtxInline(), peek(1).addChild(pop())),
                Optional(Sp(), ZeroOrMore('#'), Sp()),
                Newline()
        );
    }

    Rule AtxStart() {
        return Sequence(
                FirstOf("######", "#####", "####", "###", "##", "#"),
                push(new HeaderNode(match().length()))
        );
    }

    Rule AtxInline() {
        return Sequence(TestNot(Newline()), TestNot(Sp(), ZeroOrMore('#'), Sp(), Newline()), Inline());
    }

    Rule SetextHeading() {
        return Sequence(
                // test for successful setext heading before actually building it to save unnecessary node building
                Test(OneOrMore(SetextInline()), Newline(), FirstOf(NOrMore('=', 3), NOrMore('-', 3)), Newline()),
                FirstOf(SetextHeading1(), SetextHeading2())
        );
    }

    Rule SetextHeading1() {
        return Sequence(
                SetextInline(), push(new HeaderNode(1, pop())),
                ZeroOrMore(SetextInline(), peek(1).addChild(pop())),
                Newline(), NOrMore('=', 3), Newline()
        );
    }

    Rule SetextHeading2() {
        return Sequence(
                SetextInline(), push(new HeaderNode(2, pop())),
                ZeroOrMore(SetextInline(), peek(1).addChild(pop())),
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
                push(new BulletListNode(pop()))
        );
    }

    Rule OrderedList() {
        return Sequence(
                Test(Enumerator()),
                FirstOf(ListTight(), ListLoose()),
                push(new OrderedListNode(pop()))
        );
    }

    Rule ListTight() {
        return Sequence(
                ListItem(true), push(new Node(pop())),
                ZeroOrMore(ListItem(true), peek(1).addChild(pop())),
                ZeroOrMore(BlankLine()),
                TestNot(FirstOf(Bullet(), Enumerator()))
        );
    }

    Rule ListLoose() {
        return Sequence(
                ListItem(false), push(new Node(pop())),
                ZeroOrMore(BlankLine()),
                ZeroOrMore(ListItem(false), peek(1).addChild(pop()), ZeroOrMore(BlankLine()))
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
                innerSource.set(pop().getText()) &&
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
                                innerSource.append(blanks.getAndSet(""), pop().getText())
                        ),
                        extraNLs.set("\n\n") // if we have several lines always add two extra newlines
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
            Node innerRoot = parseRawBlock(innerSource).resultValue;
            setContext(context); // we need to save and restore the context since we might be recursing
            return push(tight ? new TightListItemNode(innerRoot) : new LooseListItemNode(innerRoot));
        }

        // ok, we have several parts, so create the root node and attach all part roots
        push(tight ? new TightListItemNode() : new LooseListItemNode());
        while (true) {
            end = innerSource.indexOf('\u0001', start);
            if (end == -1) end = innerSource.length();
            String sourcePart = innerSource.substring(start, end);

            Context<Node> context = getContext();
            Node node = parseRawBlock(sourcePart).resultValue;
            setContext(context);
            while(!node.getChildren().isEmpty()) peek().addChild(node.getChildren().get(0)); // skip one superfluous level

            if (end == innerSource.length()) return true;
            start = end + 1;
        }
    }

    Rule ListBlock() {
        StringVar source = new StringVar();
        return Sequence(
                Line(),
                source.set(pop().getText()),
                ZeroOrMore(ListBlockLine(), source.append(pop().getText())),
                push(new Node(source.get()))
        );
    }

    Rule ListBlockLine() {
        return Sequence(
                TestNot(Optional(Indent()), FirstOf(Bullet(), Enumerator())),
                TestNot(BlankLine()),
                TestNot(HorizontalRule()),
                OptionallyIndentedLine()
        );
    }

    Rule Enumerator() {
        return Sequence(NonindentSpace(), OneOrMore(Digit()), '.', OneOrMore(Spacechar()));
    }

    Rule Bullet() {
        return Sequence(TestNot(HorizontalRule()), NonindentSpace(), AnyOf("+*-"), OneOrMore(Spacechar()));
    }

    //************* HTML BLOCK ****************

    Rule HtmlBlock() {
        return Sequence(
                FirstOf(HtmlBlockInTags(), HtmlComment(), HtmlBlockSelfClosing()),
                push(new HtmlBlockNode(ext(SUPPRESS_HTML_BLOCKS) ? "" : match())),
                OneOrMore(BlankLine())
        );
    }

    Rule HtmlBlockInTags() {
        StringVar tagName = new StringVar();
        return Sequence(
                HtmlBlockOpen(), tagName.set(pop().getText()),
                ZeroOrMore(FirstOf(HtmlBlockInTags(), Sequence(TestNot(HtmlBlockClose(tagName)), ANY))),
                HtmlBlockClose(tagName)
        );
    }

    Rule HtmlBlockSelfClosing() {
        return Sequence('<', Spn1(), DefinedHtmlTagName(), drop(), Spn1(), ZeroOrMore(HtmlAttribute()), Optional('/'),
                Spn1(), '>');
    }

    Rule HtmlBlockOpen() {
        return Sequence('<', Spn1(), DefinedHtmlTagName(), Spn1(), ZeroOrMore(HtmlAttribute()), '>');
    }

    @DontSkipActionsInPredicates
    Rule HtmlBlockClose(StringVar tagName) {
        return Sequence('<', Spn1(), '/', OneOrMore(Alphanumeric()), match().equals(tagName.get()), Spn1(), '>');
    }

    Rule DefinedHtmlTagName() {
        StringVar name = new StringVar();
        return Sequence(
                OneOrMore(Alphanumeric()),
                name.set(match().toLowerCase()) && // compare ignoring case
                        Arrays.binarySearch(HTML_TAGS, name.get()) >= 0 && // make sure its a defined tag
                        push(new Node(name.get()))
        );
    }

    //************* INLINES ****************

    Rule Inlines() {
        return Sequence(
                InlineOrIntermediateEndline(), push(new Node(pop())),
                ZeroOrMore(InlineOrIntermediateEndline(), peek(1).addChild(pop())),
                Optional(Endline())
        );
    }

    Rule InlineOrIntermediateEndline() {
        return FirstOf(
                Sequence(TestNot(Endline()), Inline()),
                Sequence(Endline(), Test(Inline()))
        );
    }

    @MemoMismatches
    Rule Inline() {
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
    Rule Endline() {
        return FirstOf(LineBreak(), TerminalEndline(), NormalEndline());
    }

    Rule LineBreak() {
        return Sequence("  ", NormalEndline(), poke(new SimpleNode(LINEBREAK)));
    }

    Rule TerminalEndline() {
        return Sequence(Sp(), Newline(), EOI, push(new TextNode("\n")));
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
                ext(HARDWRAPS) ? toRule(push(new SimpleNode(LINEBREAK))) : toRule(push(new TextNode("\n")))
        );
    }

    //************* EMPHASIS / STRONG ****************

    // This keeps the parser from getting bogged down on long strings of '*' or '_',
    // or strings of '*' or '_' with space on each side:

    @MemoMismatches
    Rule UlOrStarLine() {
        return Sequence(
                FirstOf(CharLine('_'), CharLine('*')),
                push(new TextNode(match()))
        );
    }

    Rule CharLine(char c) {
        return FirstOf(NOrMore(c, 4), Sequence(Spacechar(), OneOrMore(c), Test(Spacechar())));
    }

    Rule Emph() {
        return Sequence(
                FirstOf(EmphOrStrong("*"), EmphOrStrong("_")),
                push(new EmphNode(pop()))
        );
    }

    Rule Strong() {
        return Sequence(
                FirstOf(EmphOrStrong("**"), EmphOrStrong("__")),
                push(new StrongNode(pop()))
        );
    }

    Rule EmphOrStrong(String chars) {
        return Sequence(
                EmphOrStrongOpen(chars),
                push(new Node()),
                OneOrMore(
                        TestNot(EmphOrStrongClose(chars)), TestNot(Newline()),
                        Inline(), peek(1).addChild(pop())
                ),
                EmphOrStrongClose(chars)
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
                chars.length() == 1 ? TestNot(EmphOrStrong(chars + chars)) : EMPTY,
                chars,
                TestNot(Alphanumeric())
        );
    }

    //************* LINKS ****************

    Rule Image() {
        return Sequence('!',
                FirstOf(
                        Sequence(ExplicitLink(), push(((ExpLinkNode) pop()).asImage())),
                        Sequence(ReferenceLink(), push(((RefLinkNode) pop()).asImage()))
                )
        );
    }

    @MemoMismatches
    Rule Link() {
        return FirstOf(ExplicitLink(), ReferenceLink(), AutoLinkUrl(), AutoLinkEmail());
    }

    Rule ExplicitLink() {
        Var<ExpLinkNode> node = new Var<ExpLinkNode>();
        return Sequence(
                Label(), push(node.setAndGet(new ExpLinkNode(pop()))),
                Spn1(), '(', Sp(),
                Source(node),
                Spn1(), Optional(Title(node)),
                Sp(), ')'
        );
    }

    Rule ReferenceLink() {
        Var<RefLinkNode> node = new Var<RefLinkNode>();
        return Sequence(
                Label(), push(node.setAndGet(new RefLinkNode(pop()))),
                FirstOf(
                        // regular reference link
                        Sequence(Spn1(), node.get().setSeparatorSpace(match()),
                                Label(), node.get().setReferenceKey(pop())),

                        // implicit reference link
                        Sequence(Spn1(), node.get().setSeparatorSpace(match()), "[]"),

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
                                        Sequence('\\', AnyOf("()"), url.append(matchedChar())),
                                        Sequence(TestNot(AnyOf("()>")), Nonspacechar(), url.append(matchedChar()))
                                )
                        ),
                        node.get().setUrl(url.get())
                ),
                EMPTY
        );
    }

    Rule Title(Var<ExpLinkNode> node) {
        return FirstOf(Title('\'', node), Title('"', node));
    }

    Rule Title(char delimiter, Var<ExpLinkNode> node) {
        return Sequence(
                delimiter,
                ZeroOrMore(TestNot(delimiter, Sp(), FirstOf(')', Newline())), TestNot(Newline()), ANY),
                node.get().setTitle(match()),
                delimiter
        );
    }

    Rule AutoLinkUrl() {
        return Sequence(
                ext(AUTOLINKS) ? Optional('<') : Ch('<'),
                Sequence(OneOrMore(Letter()), "://", AutoLinkEnd()),
                push(new AutoLinkNode(match())),
                ext(AUTOLINKS) ? Optional('>') : Ch('>')
        );
    }

    Rule AutoLinkEmail() {
        return Sequence(
                ext(AUTOLINKS) ? Optional('<') : Ch('<'),
                Sequence(OneOrMore(FirstOf(Alphanumeric(), AnyOf("-+_."))), '@', AutoLinkEnd()),
                push(new MailLinkNode(match())),
                ext(AUTOLINKS) ? Optional('>') : Ch('>')
        );
    }

    Rule AutoLinkEnd() {
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

    Rule Reference() {
        Var<ReferenceNode> ref = new Var<ReferenceNode>();
        return Sequence(
                NonindentSpace(), Label(), push(ref.setAndGet(new ReferenceNode(pop()))),
                ':', Spn1(), RefSrc(ref),
                Sp(), Optional(RefTitle(ref)),
                Sp(), Newline(),
                ZeroOrMore(BlankLine()),
                references.add(ref.get())
        );
    }

    Rule Label() {
        return Sequence(
                '[',
                push(new Node()),
                OneOrMore(TestNot(']'), Inline(), peek(1).addChild(pop())),
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
        return Sequence(OneOrMore(TestNot('>'), Nonspacechar()), ref.get().setUrl(match()));
    }

    Rule RefTitle(Var<ReferenceNode> ref) {
        return FirstOf(RefTitle('\'', '\'', ref), RefTitle('"', '"', ref), RefTitle('(', ')', ref));
    }

    Rule RefTitle(char open, char close, Var<ReferenceNode> ref) {
        return Sequence(
                open,
                ZeroOrMore(TestNot(close, Sp(), Newline()), TestNot(Newline()), ANY),
                ref.get().setTitle(match()),
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
                                Sequence(TestNot(Sp(), ticks),
                                        FirstOf(Spacechar(), Sequence(Newline(), TestNot(BlankLine()))))
                        )
                ),
                push(new CodeNode(match())),
                Sp(), ticks
        );
    }

    Rule Ticks(int count) {
        return Sequence(StringUtils.repeat('`', count), TestNot('`'));
    }

    //************* RAW HTML ****************

    Rule InlineHtml() {
        return Sequence(
                FirstOf(HtmlComment(), HtmlTag()),
                push(new TextNode(ext(SUPPRESS_INLINE_HTML) ? "" : match()))
        );
    }

    Rule HtmlComment() {
        return Sequence("<!--", ZeroOrMore(TestNot("-->"), ANY), "-->");
    }

    Rule HtmlTag() {
        return Sequence('<', Spn1(), Optional('/'), OneOrMore(Alphanumeric()), Spn1(), ZeroOrMore(HtmlAttribute()),
                Optional('/'), Spn1(), '>');
    }

    Rule HtmlAttribute() {
        return Sequence(
                OneOrMore(FirstOf(Alphanumeric(), '-')),
                Spn1(),
                Optional('=', Spn1(), FirstOf(Quoted(), OneOrMore(TestNot('>'), Nonspacechar()))),
                Spn1()
        );
    }

    Rule Quoted() {
        return FirstOf(
                Sequence('"', ZeroOrMore(TestNot('"'), ANY), '"'),
                Sequence('\'', ZeroOrMore(TestNot('\''), ANY), '\'')
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
                ZeroOrMore(TestNot('\r'), TestNot('\n'), ANY), line.set(match() + '\n'),
                Newline(),
                push(new Node(line.get()))
        );
    }

    //************* ENTITIES ****************

    Rule Entity() {
        return Sequence(FirstOf(HexEntity(), DecEntity(), CharEntity()), push(new TextNode(match())));
    }

    Rule HexEntity() {
        return Sequence("&", IgnoreCase("x"), OneOrMore(FirstOf(Digit(), CharRange('a', 'f'), CharRange('A', 'F'))),
                ';');
    }

    Rule DecEntity() {
        return Sequence("&#", OneOrMore(Digit()), ';');
    }

    Rule CharEntity() {
        return Sequence('&', OneOrMore(Alphanumeric()), ';');
    }

    //************* BASICS ****************

    Rule Str() {
        return Sequence(OneOrMore(NormalChar()), push(new TextNode(match())));
    }

    Rule Space() {
        return Sequence(OneOrMore(Spacechar()), push(new TextNode(" ")));
    }

    Rule Spn1() {
        return Sequence(Sp(), Optional(Newline(), Sp()));
    }

    Rule Sp() {
        return ZeroOrMore(Spacechar());
    }

    Rule Spacechar() {
        return AnyOf(" \t");
    }

    Rule Nonspacechar() {
        return Sequence(TestNot(Spacechar()), TestNot(Newline()), ANY);
    }

    @MemoMismatches
    Rule NormalChar() {
        return Sequence(TestNot(FirstOf(SpecialChar(), Spacechar(), Newline())), ANY);
    }

    Rule EscapedChar() {
        return Sequence('\\', TestNot(Newline()), ANY, push(new TextNode(match())));
    }

    Rule Symbol() {
        return Sequence(SpecialChar(), push(new TextNode(match())));
    }

    Rule SpecialChar() {
        String chars = "*_`&[]<>!\\";
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
    Rule Newline() {
        return FirstOf('\n', Sequence('\r', Optional('\n')));
    }

    Rule NonindentSpace() {
        return FirstOf("   ", "  ", " ", EMPTY);
    }

    Rule Indent() {
        return FirstOf('\t', "    ");
    }

    Rule Alphanumeric() {
        return FirstOf(Letter(), Digit());
    }

    Rule Letter() {
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'));
    }

    Rule Digit() {
        return CharRange('0', '9');
    }

    //************* ABBREVIATIONS ****************

    Rule Abbreviation() {
        Var<AbbreviationNode> node = new Var<AbbreviationNode>();
        return Sequence(
                NonindentSpace(), '*', Label(), push(node.setAndGet(new AbbreviationNode(pop()))),
                Sp(), ':', Sp(), AbbreviationText(node),
                ZeroOrMore(BlankLine()),
                abbreviations.add(node.get())
        );
    }

    Rule AbbreviationText(Var<AbbreviationNode> node) {
        return Sequence(
                node.get().setExpansion(new Node()),
                ZeroOrMore(TestNot(Newline()), Inline(), node.get().getExpansion().addChild(pop()))
        );
    }

    //************* TABLES ****************

    Rule Table() {
        Var<TableNode> node = new Var<TableNode>();
        Var<Boolean> header = new Var<Boolean>(false);
        Var<Boolean> body = new Var<Boolean>(false);
        return Sequence(
                push(node.setAndGet(new TableNode())),
                ZeroOrMore(
                        TableRow(),
                        node.get().addChild(((TableRowNode) pop()).asHeader()) && header.set(true)
                ),
                TableDivider(node),
                ZeroOrMore(TableRow(), node.get().addChild(pop()) && body.set(true)),
                header.get() || body.get() // only accept as table if we have at least one header or at least one body
        );
    }

    Rule TableDivider(Var<TableNode> tableNode) {
        Var<Boolean> pipeSeen = new Var<Boolean>(Boolean.FALSE);
        return Sequence(
                Optional('|', pipeSeen.set(Boolean.TRUE)),
                OneOrMore(TableColumn(tableNode, pipeSeen)),
                pipeSeen.get() || tableNode.get().hasTwoOrMoreDividers(),
                Sp(), Newline()
        );
    }

    Rule TableColumn(Var<TableNode> tableNode, Var<Boolean> pipeSeen) {
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

    Rule TableRow() {
        Var<Boolean> leadingPipe = new Var<Boolean>(Boolean.FALSE);
        return Sequence(
                push(new TableRowNode()),
                Optional('|', leadingPipe.set(Boolean.TRUE)),
                OneOrMore(TableCell(), peek(1).addChild(pop())),
                leadingPipe.get() || peek().getChildren().size() > 1 || match().endsWith("|"),
                Sp(), Newline()
        );
    }

    Rule TableCell() {
        return Sequence(
                push(new TableCellNode()),
                TestNot(Sp(), Optional(':'), Sp(), OneOrMore('-'), Sp(), Optional(':'), Sp(), FirstOf('|', Newline())),
                Optional(Sp(), TestNot('|'), TestNot(Newline())),
                OneOrMore(
                        TestNot('|'), TestNot(Sp(), Newline()), Inline(),
                        peek(1).addChild(pop()),
                        Optional(Sp(), Test('|'), Test(Newline()))
                ),
                ZeroOrMore('|'), ((TableCellNode) peek()).setColSpan(Math.max(1, match().length()))
        );
    }

    //************* SMARTS ****************

    Rule Smarts() {
        return FirstOf(
                Sequence(FirstOf("...", ". . ."), push(new SimpleNode(ELLIPSIS))),
                Sequence("---", push(new SimpleNode(EMDASH))),
                Sequence("--", push(new SimpleNode(ENDASH))),
                Sequence('\'', push(new SimpleNode(APOSTROPHE)))
        );
    }

    //************* QUOTES ****************

    Rule SingleQuoted() {
        return Sequence(
                SingleQuoteStart(),
                push(new QuotedNode("&lsquo;", "&rsquo;")),
                OneOrMore(TestNot(SingleQuoteEnd()), Inline(), peek(1).addChild(pop())),
                SingleQuoteEnd()
        );
    }

    Rule SingleQuoteStart() {
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

    Rule SingleQuoteEnd() {
        return Sequence('\'', TestNot(Alphanumeric()));
    }

    Rule DoubleQuoted() {
        return Sequence(
                '"',
                push(new QuotedNode("&ldquo;", "&rdquo;")),
                OneOrMore(TestNot('"'), Inline(), peek(1).addChild(pop())),
                '"'
        );
    }

    Rule DoubleAngleQuoted() {
        return Sequence(
                "<<",
                push(new QuotedNode("&laquo;", "&raquo;")),
                Optional(Spacechar(), peek().addChild(new SimpleNode(NBSP))),
                OneOrMore(
                        FirstOf(
                                Sequence(OneOrMore(Spacechar()), Test(">>"), peek().addChild(new SimpleNode(NBSP))),
                                Sequence(TestNot(">>"), Inline(), peek(1).addChild(pop()))
                        )
                ),
                ">>"
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
        ParsingResult<Node> result = parseRunnerFactory.create().run(text);
        if (!result.matched) {
            String errorMessage = "Internal error";
            if (result.hasErrors()) errorMessage += ": " + result.parseErrors.get(0);
            throw new RuntimeException(errorMessage);
        }
        return result;
    }

}
