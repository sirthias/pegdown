package org.pegdown;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.Cached;
import org.parboiled.common.StringUtils;
import org.parboiled.google.base.Function;

import java.util.ArrayList;
import java.util.List;

import static org.parboiled.trees.TreeUtils.addChild;

@SuppressWarnings({"InfiniteRecursion"})
public class MarkDownParser extends BaseParser<AstNode> implements AstNodeType {

    static final String[] HTML_TAGS = new String[]{"address", "blockquote", "center", "dir", "div", "dl",
            "fieldset", "form", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "isindex", "menu", "noframes", "noscript",
            "ol", "p", "pre", "table", "ul", "dd", "dt", "frameset", "li", "tbody", "td", "tfoot", "th", "thead", "tr",
            "script", "style"};

    final List<AstNode> references = new ArrayList<AstNode>();

    Rule Doc() {
        return Sequence(
                create(DEFAULT),
                ZeroOrMore(
                        Sequence(Block(), UP2(attach(lastValue())))
                )
        );
    }

    Rule Block() {
        return Sequence(
                ZeroOrMore(BlankLine()),
                FirstOf(BlockQuote(), Verbatim(), Note(), Reference(), HorizontalRule(), Heading(), OrderedList(),
                        BulletList(), HtmlBlock(), Para(), Plain())
        );
    }

    Rule Para() {
        return Sequence(
                Sequence(NonindentSpace(), Inlines(), set(), OneOrMore(BlankLine())),
                set(lastValue().setType(PARA))
        );
    }

    Rule Plain() {
        return Sequence(Inlines(), set(lastValue().setType(PLAIN)));
    }

    Rule BlockQuote() {
        return OneOrMore(
                Sequence(
                        '>', Optional(' '), Line(), UP(create(BLOCKQUOTE, lastValue())),
                        ZeroOrMore(
                                Sequence(
                                        TestNot('>'),
                                        TestNot(BlankLine()),
                                        Line(), UP3(attach(lastValue()))
                                )
                        ),
                        ZeroOrMore(
                                Sequence(BlankLine(), UP3(attach(lastValue())))
                        )
                )
        );
    }

    Rule Verbatim() {
        return OneOrMore(
                Sequence(
                        ZeroOrMore(BlankLine()),
                        NonblankIndentedLine(), UP(create(VERBATIM, lastValue())),
                        ZeroOrMore(
                                Sequence(NonblankIndentedLine(), UP3(attach(lastValue())))
                        )
                )
        );
    }

    Rule HorizontalRule() {
        return Sequence(
                NonindentSpace(),
                FirstOf(HorizontalRule('*'), HorizontalRule('-'), HorizontalRule('_')),
                Sp(), Newline(), OneOrMore(BlankLine()),
                create(HRULE)
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
                        Sequence(AtxInline(), UP2(attach(lastValue())))
                ),
                Optional(Sequence(Sp(), ZeroOrMore('#'), Sp())),
                Newline()
        );
    }

    Rule AtxStart() {
        return Sequence(
                FirstOf("######", "#####", "####", "###", "##", "#"),
                create(H1 + lastText().length() - 1)
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
                SetextInline(), create(H1, lastValue()),
                OneOrMore(
                        Sequence(SetextInline(), UP2(attach(lastValue())))
                ),
                Newline(), NOrMore('=', 3), Newline()
        );
    }

    Rule SetextHeading2() {
        return Sequence(
                SetextInline(), create(H2, lastValue()),
                OneOrMore(
                        Sequence(SetextInline(), UP2(attach(lastValue())))
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
                set(lastValue().setType(LIST_BULLET))
        );
    }

    Rule OrderedList() {
        return Sequence(
                Test(Enumerator()),
                FirstOf(ListTight(), ListLoose()),
                set(lastValue().setType(LIST_ORDERED))
        );
    }

    Rule ListTight() {
        return Sequence(
                ListItem(), create(DEFAULT, lastValue().setType(LISTITEM_TIGHT)),
                ZeroOrMore(
                        Sequence(ListItem(), UP2(attach(lastValue().setType(LISTITEM_TIGHT))))
                ),
                ZeroOrMore(BlankLine()),
                TestNot(FirstOf(Bullet(), Enumerator()))
        );
    }

    Rule ListLoose() {
        return Sequence(
                ListItem(), create(DEFAULT, lastValue().setType(LISTITEM_LOOSE)),
                ZeroOrMore(BlankLine()),
                ZeroOrMore(
                        Sequence(
                                ListItem(),
                                UP2(attach(lastValue().setType(LISTITEM_LOOSE))),
                                ZeroOrMore(BlankLine())
                        )
                )
        );
    }

    Rule ListItem() {
        return Sequence(
                FirstOf(Bullet(), Enumerator()),
                ListBlock(), create(DEFAULT, lastValue()),
                ZeroOrMore(
                        Sequence(
                                ZeroOrMore(BlankLine()),
                                OneOrMore(
                                        Sequence(
                                                Indent(),
                                                ListBlock(),
                                                UP4(attach(lastValue()))
                                        )
                                )
                        )
                )
        );
    }

    Rule ListBlock() {
        return Sequence(
                Line(), create(LISTITEMBLOCK, lastValue()),
                ZeroOrMore(
                        Sequence(ListBlockLine(), UP2(attach(lastValue())))
                )
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
                Sequence(
                        FirstOf(HtmlBlockInTags(), HtmlComment(), HtmlBlockSelfClosing()), set(),
                        OneOrMore(BlankLine())
                ),
                create(HTMLBLOCK, lastText())
        );
    }

    Rule HtmlBlockInTags() {
        return FirstOfAllHtmlTagsFor(new Function<String, Rule>() {
            public Rule apply(String tag) {
                return Sequence(
                        HtmlBlockOpen(tag),
                        ZeroOrMore(FirstOf(HtmlBlockInTags(), Sequence(TestNot(HtmlBlockClose(tag)), Any()))),
                        HtmlBlockClose(tag)
                );
            }
        });
    }

    Rule HtmlBlockSelfClosing() {
        return Sequence('<', Spn1(), HtmlBlockType(), Spn1(), ZeroOrMore(HtmlAttribute()), '/', Spn1(), '>');
    }

    Rule HtmlBlockType() {
        return FirstOfAllHtmlTagsFor(new Function<String, Rule>() {
            public Rule apply(String tag) {
                return StringIgnoreCase(tag);
            }
        });
    }

    Rule FirstOfAllHtmlTagsFor(Function<String, Rule> function) {
        Rule[] rules = new Rule[HTML_TAGS.length];
        for (int i = 0; i < HTML_TAGS.length; i++) {
            rules[i] = function.apply(HTML_TAGS[i]);
        }
        return FirstOf(rules);
    }

    Rule HtmlBlockOpen(String name) {
        return Sequence('<', Spn1(), StringIgnoreCase(name), Spn1(), ZeroOrMore(HtmlAttribute()), '>');
    }

    Rule HtmlBlockClose(String name) {
        return Sequence('<', Spn1(), '/', StringIgnoreCase(name), Spn1(), '>');
    }

    //************* INLINES ****************

    Rule Inlines() {
        return Sequence(
                InlineOrIntermediateEndline(), create(DEFAULT, lastValue()),
                ZeroOrMore(
                        Sequence(InlineOrIntermediateEndline(), UP2(attach(lastValue())))
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
        return FirstOf(Str(), Endline(), UlOrStarLine(), Space(), Strong(), Emph(), Image(), Link(), NoteReference(),
                InlineNote(), Code(), RawHtml(), Entity(), EscapedChar(), Symbol());
    }

    Rule Endline() {
        return FirstOf(LineBreak(), TerminalEndline(), NormalEndline());
    }

    Rule LineBreak() {
        return Sequence("  ", NormalEndline(), create(LINEBREAK));
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
                create(SPACE, "\n")
        );
    }

    //************* EMPHASIS / STRONG ****************

    // This keeps the parser from getting bogged down on long strings of '*' or '_',
    // or strings of '*' or '_' with space on each side:
    Rule UlOrStarLine() {
        return Sequence(
                FirstOf(CharLine('_'), CharLine('*')),
                create(TEXT, lastText())
        );
    }

    Rule CharLine(char c) {
        return FirstOf(NOrMore(c, 4), Sequence(Spacechar(), OneOrMore(c), Test(Spacechar())));
    }

    Rule Emph() {
        return Sequence(
                FirstOf(EmphOrStrong("*"), EmphOrStrong("_")),
                set(lastValue().setType(EMPH))
        );
    }

    Rule Strong() {
        return Sequence(
                FirstOf(EmphOrStrong("**"), EmphOrStrong("__")),
                set(lastValue().setType(STRONG))
        );
    }

    Rule EmphOrStrong(String chars) {
        return Sequence(
                EmphOrStrongOpen(chars),
                create(DEFAULT),
                ZeroOrMore(
                        Sequence(
                                TestNot(EmphOrStrongClose(chars)),
                                Inline(), UP2(attach(lastValue()))
                        )
                ),
                EmphOrStrongClose(chars), attach(lastValue())
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
                chars.charAt(0) == '_' ? TestNot(Alphanumeric()) : Empty(),
                '*'
        );
    }

    //************* LINKS ****************

    Rule Image() {
        return Sequence('!', ExplicitOrReferenceLink(), set(lastValue().setType(IMAGE)));
    }

    Rule Link() {
        return FirstOf(ExplicitOrReferenceLink(), AutoLinkUrl(), AutoLinkEmail());
    }

    Rule ExplicitOrReferenceLink() {
        return Sequence(
                Label(), set(lastValue().setType(LINK)),
                FirstOf(
                        ExplicitLinkRest(),
                        Sequence(NormalReferenceLinkRest(), UP2(attach(lastValue()))),
                        Sequence(ImplicitReferenceLinkRest(), UP2(attach(lastValue())))
                )
        );
    }

    Rule ExplicitLinkRest() {
        return Sequence(
                Spn1(), '(', Sp(),
                Source(), UP2(attach(lastValue())),
                Spn1(), Optional(Sequence(Title(), UP3(attach(lastValue())))),
                Sp(), ')'
        );
    }

    Rule NormalReferenceLinkRest() {
        return Sequence(Spn1(), TestNot("[]"), Label(), set(lastValue().setType(LINK_REF)));
    }

    Rule ImplicitReferenceLinkRest() {
        return Optional(Sequence(Spn1(), "[]", create(LINK_REF)));
    }

    Rule Source() {
        return FirstOf(Sequence('<', SourceContents(), '>'), SourceContents());
    }

    Rule SourceContents() {
        return FirstOf(
                Sequence(
                        OneOrMore(Sequence(TestNot('('), TestNot(')'), TestNot('>'), Nonspacechar())),
                        create(LINK_URL, lastText())
                ),
                Sequence('(', SourceContents(), ')')
        );
    }

    Rule Title() {
        return FirstOf(Title('\''), Title('"'));
    }

    Rule Title(char delimiter) {
        return Sequence(
                delimiter,
                ZeroOrMore(
                        Sequence(TestNot(Sequence(delimiter, Sp(), FirstOf(')', Newline()))), TestNot(Newline()), Any())
                ),
                create(LINK_TITLE, lastText()),
                delimiter
        );
    }

    Rule AutoLinkUrl() {
        return Sequence(
                '<',
                Sequence(OneOrMore(Letter()), "://", OneOrMore(Sequence(TestNot(Newline()), TestNot('>'), Any()))),
                create(LINK_URL, lastText()),
                '>'
        );
    }

    Rule AutoLinkEmail() {
        return Sequence(
                '<',
                Sequence(OneOrMore(FirstOf(Alphanumeric(), CharSet("-+_"))), '@',
                        OneOrMore(Sequence(TestNot(Newline()), TestNot('>'), Any()))),
                create(LINK_URL, "mailto:" + lastText()),
                '>'
        );
    }

    //************* REFERENCE ****************

    Rule Reference() {
        return Sequence(
                NonindentSpace(), TestNot("[]"), Label(), set(lastValue().setType(REFERENCE)),
                ':', Spn1(), RefSrc(), attach(lastValue()),
                Spn1(), Optional(Sequence(RefTitle(), UP2(attach(lastValue())))),
                ZeroOrMore(BlankLine()),
                references.add(value())
        );
    }

    Rule Label() {
        return Sequence(
                '[', create(DEFAULT),
                ZeroOrMore(Sequence(TestNot(']'), Inline(), UP2(attach(lastValue())))),
                ']'
        );
    }

    Rule RefSrc() {
        return Sequence(OneOrMore(Nonspacechar()), create(LINK_URL, lastText()));
    }

    Rule RefTitle() {
        return Sequence(
                FirstOf(RefTitle('\'', '\''), RefTitle('"', '"'), RefTitle('(', ')')),
                create(LINK_TITLE, lastText())
        );
    }

    Rule RefTitle(char open, char close) {
        return Sequence(
                open,
                ZeroOrMore(
                        Sequence(TestNot(Sequence(close, Sp(), FirstOf(Newline(), Eoi()))), TestNot(Newline()), Any())
                ),
                create(LINK_TITLE, lastText()),
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
                put(),
                Sp(), ticks,
                create(CODE, text(get()))
        );
    }

    Rule Ticks(int count) {
        return Sequence(StringUtils.repeat('`', count), TestNot('`'));
    }

    //************* RAW HTML ****************

    Rule RawHtml() {
        return Sequence(FirstOf(HtmlComment(), HtmlTag()), create(HTML, lastText()));
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
        return FirstOf(
                Sequence(ZeroOrMore(Sequence(TestNot('\r'), TestNot('\n'), Any())), Newline()),
                Sequence(OneOrMore(Any()), Eoi())
        );
    }

    //************* NOTES ****************

    Rule InlineNote() {
        return Sequence("^[", OneOrMore(Sequence(TestNot(']'), Inline())), ']');
    }

    Rule NoteReference() {
        return RawNoteReference();
    }

    Rule RawNoteReference() {
        return Sequence("[^", OneOrMore(Sequence(TestNot(Newline()), TestNot(']'), Any())), ']');
    }

    Rule Note() {
        return Sequence(NonindentSpace(), RawNoteReference(), ':', Sp(), RawNoteBlock(),
                ZeroOrMore(Sequence(Test(Indent()), RawNoteBlock())));
    }

    Rule RawNoteBlock() {
        return Sequence(OneOrMore(Sequence(TestNot(BlankLine()), OptionallyIndentedLine())), ZeroOrMore(BlankLine()));
    }

    //************* ENTITIES ****************

    Rule Entity() {
        return Sequence(FirstOf(HexEntity(), DecEntity(), CharEntity()), create(HTML, lastText()));
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
                create(TEXT, lastText())
        );
    }

    Rule Space() {
        return Sequence(OneOrMore(Spacechar()), create(SPACE, " "));
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
        return Sequence('\\', TestNot(Newline()), Any(), create(TEXT, lastText()));
    }

    Rule Symbol() {
        return Sequence(SpecialChar(), create(SPECIAL, lastText()));
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

    //************* ACTIONS ****************

    boolean attach(AstNode astNode) {
        if (astNode != null) addChild(getContext().getNodeValue(), astNode);
        return true;
    }

    boolean create(int type) {
        return create(type, null, null);
    }

    boolean create(int type, String text) {
        return create(type, text, null);
    }

    boolean create(int type, AstNode child) {
        return create(type, null, child);
    }

    boolean create(int type, String text, AstNode child) {
        AstNode astNode = new AstNode().setType(type).setText(text);
        if (child != null) addChild(astNode, child);
        return set(astNode);
    }

}
