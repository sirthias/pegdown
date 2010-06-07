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
import org.parboiled.common.StringUtils;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.StringVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parboiled parser for the standard markdown syntax.
 * Builds an Abstract Syntax Tree (AST) of {@link AstNode} objects.
 */
@SuppressWarnings({"InfiniteRecursion"})
@SkipActionsInPredicates
public class MarkdownParser extends BaseParser<AstNode> implements AstNodeType {

    static final String[] HTML_TAGS = new String[]{
            "address", "blockquote", "center", "dd", "dir", "div", "dl", "dt", "fieldset", "form", "frameset", "h1",
            "h2", "h3", "h4", "h5", "h6", "hr", "isindex", "li", "menu", "noframes", "noscript", "ol", "p", "pre",
            "script", "style", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "ul"
    };

    final List<AstNode> references = new ArrayList<AstNode>();
    final List<AstNode> abbreviations = new ArrayList<AstNode>();

    @SuppressSubnodes
    Rule Doc() {
        return Sequence(
                set(new AstNode()),
                ZeroOrMore(
                        Sequence(Block(), UP2(value().addChild(prevValue())))
                )
        );
    }

    Rule Block() {
        return Sequence(
                ZeroOrMore(BlankLine()),
                FirstOf(BlockQuote(), Verbatim(), Reference(), HorizontalRule(), Heading(), OrderedList(),
                        BulletList(), HtmlBlock(), Para(), Inlines())
        );
    }

    Rule Para() {
        return Sequence(
                NonindentSpace(), Inlines(), set(prevValue().withType(PARA)), OneOrMore(BlankLine())
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
                // trigger an recursive parsing run on the innerSource we just built
                // and attach the root of the inner parses AST
                set(parseRawBlock(innerSource.get()).parseTreeRoot.getValue().withType(BLOCKQUOTE))
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
                set(new AstNode(VERBATIM).withText(text.get()))
        );
    }

    Rule HorizontalRule() {
        return Sequence(
                NonindentSpace(),
                FirstOf(HorizontalRule('*'), HorizontalRule('-'), HorizontalRule('_')),
                Sp(), Newline(), OneOrMore(BlankLine()),
                set(new AstNode(HRULE))
        );
    }

    Rule HorizontalRule(char c) {
        return Sequence(c, Sp(), c, Sp(), c, ZeroOrMore(Sequence(Sp(), c)));
    }

    //************* HEADING ****************

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
                set(new AstNode(H1 + prevText().length() - 1))
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
                SetextInline(), set(new AstNode(H1).withChild(prevValue())),
                ZeroOrMore(
                        Sequence(SetextInline(), UP2(value().addChild(prevValue())))
                ),
                Newline(), NOrMore('=', 3), Newline()
        );
    }

    Rule SetextHeading2() {
        return Sequence(
                SetextInline(), set(new AstNode(H2).withChild(prevValue())),
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
                set(prevValue().withType(BULLET_LIST))
        );
    }

    Rule OrderedList() {
        return Sequence(
                Test(Enumerator()),
                FirstOf(ListTight(), ListLoose()),
                set(prevValue().withType(ORDERED_LIST))
        );
    }

    Rule ListTight() {
        return Sequence(
                ListItem(TIGHT_LIST_ITEM), set(new AstNode().withChild(prevValue())),
                ZeroOrMore(
                        Sequence(ListItem(TIGHT_LIST_ITEM), UP2(value().addChild(prevValue())))
                ),
                ZeroOrMore(BlankLine()),
                TestNot(FirstOf(Bullet(), Enumerator()))
        );
    }

    Rule ListLoose() {
        return Sequence(
                ListItem(LOOSE_LIST_ITEM), set(new AstNode().withChild(prevValue())),
                ZeroOrMore(BlankLine()),
                ZeroOrMore(
                        Sequence(ListItem(LOOSE_LIST_ITEM), UP2(value().addChild(prevValue())), ZeroOrMore(BlankLine()))
                )
        );
    }

    Rule ListItem(int type) {
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
                        (type == TIGHT_LIST_ITEM || extraNLs.set("\n\n")), // append extra \n\n to loose list items

                ZeroOrMore(
                        Sequence(
                                FirstOf(
                                        // if we have blank lines append them to the inner source
                                        OneOrMore(Sequence(BlankLine(), blanks.append("\n"))),

                                        // if we do not have a blank line we append a boundary marker
                                        blanks.set(type == TIGHT_LIST_ITEM ? "\u0001" : "\n\n\u0001")
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
                setListItemNode(type, innerSource.get() + extraNLs.get())
        );
    }

    // special action running the inner parse for list node source
    // the innerSource can contain \u0001 boundary markers, which indicate, where to split the innerSource
    // and run independent inner parsing runs
    boolean setListItemNode(int type, String innerSource) {
        int start = 0;
        int end = innerSource.indexOf('\u0001', start); // look for boundary markers
        if (end == -1) {
            // if we have just one part simply parse and set
            Context<AstNode> context = getContext();
            AstNode astNode = parseRawBlock(innerSource).parseTreeRoot.getValue();
            setContext(context); // we need to save and restore the context since we might be recursing
            return set(astNode.withType(type));
        }

        // ok, we have several parts, so create the root node and attach all part roots
        set(new AstNode(type));
        while (true) {
            end = innerSource.indexOf('\u0001', start);
            if (end == -1) end = innerSource.length();
            String sourcePart = innerSource.substring(start, end);

            Context<AstNode> context = getContext();
            AstNode astNode = parseRawBlock(sourcePart).parseTreeRoot.getValue();
            setContext(context);
            value().addChild(astNode.getChildren().get(0)); // skip one superfluous level

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
                set(new AstNode().withText(source.get()))
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
                set(new AstNode(HTMLBLOCK).withText(prevText())),
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
                        set(new AstNode().withText(name.get()))
        );
    }

    //************* INLINES ****************

    Rule Inlines() {
        return Sequence(
                InlineOrIntermediateEndline(), set(new AstNode().withChild(prevValue())),
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
        return FirstOf(Str(), Endline(), UlOrStarLine(), Space(), Strong(), Emph(), Image(), Link(), Code(), RawHtml(),
                Entity(), EscapedChar(), Symbol());
    }

    Rule Endline() {
        return FirstOf(LineBreak(), TerminalEndline(), NormalEndline());
    }

    Rule LineBreak() {
        return Sequence("  ", NormalEndline(), set(new AstNode(LINEBREAK)));
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
                set(new AstNode(SPACE).withText("\n"))
        );
    }

    //************* EMPHASIS / STRONG ****************

    // This keeps the parser from getting bogged down on long strings of '*' or '_',
    // or strings of '*' or '_' with space on each side:
    Rule UlOrStarLine() {
        return Sequence(
                FirstOf(CharLine('_'), CharLine('*')),
                set(new AstNode(TEXT).withText(prevText()))
        );
    }

    Rule CharLine(char c) {
        return FirstOf(NOrMore(c, 4), Sequence(Spacechar(), OneOrMore(c), Test(Spacechar())));
    }

    Rule Emph() {
        return Sequence(
                FirstOf(EmphOrStrong("*"), EmphOrStrong("_")),
                set(prevValue().withType(EMPH))
        );
    }

    Rule Strong() {
        return Sequence(
                FirstOf(EmphOrStrong("**"), EmphOrStrong("__")),
                set(prevValue().withType(STRONG))
        );
    }

    Rule EmphOrStrong(String chars) {
        return Sequence(
                EmphOrStrongOpen(chars),
                set(new AstNode()),
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
                        Sequence(ExplicitLink(), set(prevValue().withType(EXP_IMG_LINK))),
                        Sequence(ReferenceLink(), set(prevValue().withType(REF_IMG_LINK)))
                )
        );
    }

    Rule Link() {
        return FirstOf(ExplicitLink(), ReferenceLink(), AutoLinkUrl(), AutoLinkEmail());
    }

    Rule ExplicitLink() {
        return Sequence(
                Label(), set(new AstNode(EXP_LINK).withChild(prevValue())),
                Spn1(), '(', Sp(),
                Source(), value().addChild(prevValue()),
                Spn1(), Optional(Sequence(Title(), UP2(value().addChild(prevValue())))),
                Sp(), ')'
        );
    }

    Rule ReferenceLink() {
        StringVar blanks = new StringVar();
        return Sequence(
                Label(), set(new AstNode(REF_LINK).withChild(prevValue())),
                FirstOf(
                        // regular reference link
                        Sequence(Spn1(), blanks.set(prevText()), TestNot("[]"), Label(),
                                set(prevValue().withType(LINK_REF))),

                        // implicit reference link
                        Optional(
                                Sequence(Spn1(), blanks.set(prevText()), "[]", set(new AstNode(LINK_REF).withText("")))
                        )
                ),
                value().addChild(new AstNode(SPACE).withText(blanks.get())) && value().addChild(prevValue())
        );
    }

    Rule Source() {
        StringVar url = new StringVar("");
        return FirstOf(
                Sequence('(', Source(), ')'),
                Sequence('<', Source(), '>'),
                Sequence(
                        OneOrMore(
                                FirstOf(
                                        Sequence('\\', CharSet("()"), url.append(prevChar())),
                                        Sequence(TestNot(CharSet("()>")), Nonspacechar(), url.append(prevChar()))
                                )
                        ),
                        set(new AstNode(LINK_URL).withText(url.get()))
                ),
                Empty()
        );
    }

    Rule Title() {
        return FirstOf(Title('\''), Title('"'));
    }

    Rule Title(char delimiter) {
        return Sequence(
                CharSet("\'\""),
                ZeroOrMore(
                        Sequence(TestNot(Sequence(delimiter, Sp(), FirstOf(')', Newline()))), TestNot(Newline()), Any())
                ),
                set(new AstNode(LINK_TITLE).withText(prevText())),
                delimiter
        );
    }

    Rule AutoLinkUrl() {
        return Sequence(
                '<',
                Sequence(OneOrMore(Letter()), "://", OneOrMore(Sequence(TestNot(Newline()), TestNot('>'), Any()))),
                set(new AstNode(AUTO_LINK).withText(prevText())),
                '>'
        );
    }

    Rule AutoLinkEmail() {
        return Sequence(
                '<',
                Sequence(OneOrMore(FirstOf(Alphanumeric(), CharSet("-+_"))), '@',
                        OneOrMore(Sequence(TestNot(Newline()), TestNot('>'), Any()))),
                set(new AstNode(MAIL_LINK).withText(prevText())),
                '>'
        );
    }

    //************* REFERENCE ****************

    Rule Reference() {
        return Sequence(
                NonindentSpace(), TestNot("[]"), Label(), set(new AstNode(REFERENCE).withChild(prevValue())),
                ':', Sp(), RefSrc(), value().addChild(prevValue()),
                Spn1(), Optional(Sequence(RefTitle(), UP2(value().addChild(prevValue())))),
                ZeroOrMore(BlankLine()),
                references.add(value())
        );
    }

    Rule Label() {
        return Sequence(
                '[', set(new AstNode(LINK_LABEL)),
                ZeroOrMore(Sequence(TestNot(']'), Inline(), UP2(value().addChild(prevValue())))),
                ']'
        );
    }

    Rule RefSrc() {
        return FirstOf(
                Sequence('<', RefSrcContent(), '>'),
                RefSrcContent()
        );
    }

    Rule RefSrcContent() {
        return Sequence(OneOrMore(Sequence(TestNot('>'), Nonspacechar())),
                set(new AstNode(LINK_URL).withText(prevText())));
    }

    Rule RefTitle() {
        return FirstOf(RefTitle('\'', '\''), RefTitle('"', '"'), RefTitle('(', ')'));
    }

    Rule RefTitle(char open, char close) {
        return Sequence(
                open,
                ZeroOrMore(
                        Sequence(TestNot(Sequence(close, Sp(), FirstOf(Newline(), Eoi()))), TestNot(Newline()), Any())
                ),
                set(new AstNode(LINK_TITLE).withText(prevText())),
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
                set(new AstNode(CODE).withText(prevText())),
                Sp(), ticks
        );
    }

    Rule Ticks(int count) {
        return Sequence(StringUtils.repeat('`', count), TestNot('`'));
    }

    //************* RAW HTML ****************

    Rule RawHtml() {
        return Sequence(FirstOf(HtmlComment(), HtmlTag()), set(new AstNode(HTML).withText(prevText())));
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
                set(new AstNode(DEFAULT).withText(line.get()))
        );
    }

    //************* ENTITIES ****************

    Rule Entity() {
        return Sequence(FirstOf(HexEntity(), DecEntity(), CharEntity()), set(new AstNode(HTML).withText(prevText())));
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
                set(new AstNode(TEXT).withText(prevText()))
        );
    }

    Rule Space() {
        return Sequence(OneOrMore(Spacechar()), set(new AstNode(SPACE).withText(" ")));
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
        return Sequence('\\', TestNot(Newline()), Any(), set(new AstNode(TEXT).withText(prevText())));
    }

    Rule Symbol() {
        return Sequence(SpecialChar(), set(new AstNode(SPECIAL).withText(prevText())));
    }

    Rule SpecialChar() {
        return CharSet("*_`&[]<!\\");
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

    //************* HELPERS ****************

    Rule NOrMore(char c, int n) {
        return Sequence(StringUtils.repeat(c, n), ZeroOrMore(c));
    }

    ParsingResult<AstNode> parseRawBlock(String text) {
        ParsingResult<AstNode> result = ReportingParseRunner.run(Doc(), text);
        if (!result.matched) {
            String errorMessage = "Internal error";
            if (result.hasErrors()) errorMessage += ": " + result.parseErrors.get(0);
            throw new RuntimeException(errorMessage);
        }
        return result;
    }

}
