package org.pegdown;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.common.StringUtils;
import org.parboiled.google.base.Function;

@SuppressWarnings({"InfiniteRecursion"})
public class PegDownParser extends BaseParser {

    private String[] HTML_TAGS = new String[]{"address", "blockquote", "center", "dir", "div", "dl", "fieldset", "form",
            "h1", "h2", "h3", "h4", "h5", "h6", "hr", "isindex", "menu", "noframes", "noscript", "ol", "p", "pre",
            "table", "ul", "dd", "dt", "frameset", "li", "tbody", "td", "tfoot", "th", "thead", "tr", "script"};

    Rule Doc() {
        return zeroOrMore(Block());
    }

    Rule Block() {
        return sequence(zeroOrMore(BlankLine()), firstOf(
                BlockQuote(), Verbatim(), Note(), Reference(), HorizontalRule(), Heading(), OrderedList(), BulletList(),
                HtmlBlock(), StyleBlock(), Para(), Plain()
        ));
    }

    Rule Para() {
        return sequence(NonindentSpace(), Inlines(), oneOrMore(BlankLine()));
    }

    Rule Plain() {
        return Inlines();
    }

    Rule BlockQuote() {
        return oneOrMore(sequence(
                '>', optional(' '), Line(),
                zeroOrMore(sequence(testNot('>'), testNot(BlankLine()), Line())),
                zeroOrMore(BlankLine())
        ));
    }

    Rule Verbatim() {
        return oneOrMore(VerbatimChunk());
    }

    Rule VerbatimChunk() {
        return sequence(zeroOrMore(BlankLine()), oneOrMore(NonblankIndentedLine()));
    }

    Rule HorizontalRule() {
        return sequence(NonindentSpace(), firstOf(
                sequence('*', Sp(), '*', Sp(), '*', zeroOrMore(sequence(Sp(), '*'))),
                sequence('-', Sp(), '-', Sp(), '-', zeroOrMore(sequence(Sp(), '-'))),
                sequence('_', Sp(), '_', Sp(), '_', zeroOrMore(sequence(Sp(), '_')))
        ), Sp(), Newline(), oneOrMore(BlankLine()));
    }

    //************* HEADING ****************

    Rule Heading() {
        return firstOf(AtxHeading(), SetextHeading());
    }

    Rule AtxHeading() {
        return sequence(AtxStart(), Sp(), oneOrMore(AtxInline()), optional(sequence(Sp(), zeroOrMore('#'), Sp())),
                Newline());
    }

    Rule AtxStart() {
        return firstOf("######", "#####", "####", "###", "##", "#");
    }

    Rule AtxInline() {
        return sequence(testNot(Newline()), testNot(sequence(Sp(), zeroOrMore('#'), Sp(), Newline())), Inline());
    }

    Rule SetextHeading() {
        return firstOf(SetextHeading1(), SetextHeading2());
    }

    Rule SetextHeading1() {
        return sequence(oneOrMore(sequence(testNot(Endline()), Inline())), Newline(), NOrMore('=', 3), Newline());
    }

    Rule SetextHeading2() {
        return sequence(oneOrMore(sequence(testNot(Endline()), Inline())), Newline(), NOrMore('-', 3), Newline());
    }

    //************* LISTS ****************

    Rule BulletList() {
        return sequence(test(Bullet()), firstOf(ListTight(), ListLoose()));
    }

    Rule OrderedList() {
        return sequence(test(Enumerator()), firstOf(ListTight(), ListLoose()));
    }

    Rule ListTight() {
        return sequence(oneOrMore(ListItem()), zeroOrMore(BlankLine()), testNot(firstOf(Bullet(), Enumerator())));
    }

    Rule ListLoose() {
        return oneOrMore(sequence(ListItem(), zeroOrMore(BlankLine())));
    }

    Rule ListItem() {
        return sequence(firstOf(Bullet(), Enumerator()), ListBlock(), zeroOrMore(ListContinuationBlock()));
    }

    Rule ListContinuationBlock() {
        return sequence(zeroOrMore(BlankLine()), oneOrMore(sequence(Indent(), ListBlock())));
    }

    Rule ListBlock() {
        return sequence(Line(), zeroOrMore(ListBlockLine()));
    }

    Rule ListBlockLine() {
        return sequence(testNot(sequence(optional(Indent()), firstOf(Bullet(), Enumerator()))), testNot(BlankLine()),
                testNot(HorizontalRule()), OptionallyIndentedLine());
    }

    Rule Enumerator() {
        return sequence(NonindentSpace(), oneOrMore(Digit()), '.', oneOrMore(Spacechar()));
    }

    Rule Bullet() {
        return sequence(testNot(HorizontalRule()), NonindentSpace(), charSet("+*-"), oneOrMore(Spacechar()));
    }

    //************* HTML BLOCK ****************

    Rule HtmlBlock() {
        return sequence(firstOf(HtmlBlockInTags(), HtmlComment(), HtmlBlockSelfClosing()), oneOrMore(BlankLine()));
    }

    Rule HtmlBlockInTags() {
        return FirstOfAllHtmlTagsFor(new Function<String, Rule>() {
            public Rule apply(String tag) {
                return sequence(
                        HtmlBlockOpen(tag),
                        zeroOrMore(firstOf(HtmlBlockInTags(), sequence(testNot(HtmlBlockClose(tag)), any()))),
                        HtmlBlockClose(tag)
                );
            }
        });
    }

    Rule HtmlBlockSelfClosing() {
        return sequence('<', Spn1(), HtmlBlockType(), Spn1(), zeroOrMore(HtmlAttribute()), '/', Spn1(), '>');
    }

    Rule HtmlBlockType() {
        return FirstOfAllHtmlTagsFor(new Function<String, Rule>() {
            public Rule apply(String tag) {
                return stringIgnoreCase(tag);
            }
        });
    }

    Rule FirstOfAllHtmlTagsFor(Function<String, Rule> function) {
        Rule[] rules = new Rule[HTML_TAGS.length];
        for (int i = 0; i < HTML_TAGS.length; i++) {
            rules[i] = function.apply(HTML_TAGS[i]);
        }
        return firstOf(rules);
    }

    Rule StyleBlock() {
        return sequence(InStyleTags(), zeroOrMore(BlankLine()));
    }

    Rule InStyleTags() {
        return sequence(HtmlBlockOpen("style"), zeroOrMore(sequence(testNot(HtmlBlockClose("style")), any())),
                HtmlBlockClose("style"));
    }

    Rule HtmlBlockOpen(String name) {
        return sequence('<', Spn1(), stringIgnoreCase(name), Spn1(), zeroOrMore(HtmlAttribute()), '>');
    }

    Rule HtmlBlockClose(String name) {
        return sequence('<', Spn1(), '/', stringIgnoreCase(name), Spn1(), '>');
    }

    //************* INLINES ****************

    Rule Inlines() {
        return sequence(
                oneOrMore(firstOf(sequence(testNot(Endline()), Inline()), sequence(Endline(), test(Inline())))),
                optional(Endline())
        );
    }

    Rule Inline() {
        return firstOf(Str(), Endline(), UlOrStarLine(), Space(), Strong(), Emph(), Image(), Link(), NoteReference(),
                InlineNote(), Code(), RawHtml(), Entity(), EscapedChar(), Ellipsis(), EmDash(), EnDash(),
                SingleQuoted(), DoubleQuoted(), Apostrophe(), SpecialChar());
    }

    Rule Endline() {
        return firstOf(LineBreak(), TerminalEndline(), NormalEndline());
    }

    Rule NormalEndline() {
        return sequence(Sp(), Newline(), testNot(firstOf(
                BlankLine(), '>', AtxStart(), sequence(Line(), firstOf(NOrMore('=', 3), NOrMore('-', 3)), Newline())
        )));
    }

    Rule TerminalEndline() {
        return sequence(Sp(), Newline(), eoi());
    }

    Rule LineBreak() {
        return sequence("  ", NormalEndline());
    }

    //************* EMPHASIS / STRONG ****************

    // This keeps the parser from getting bogged down on long strings of '*' or '_',
    // or strings of '*' or '_' with space on each side:
    Rule UlOrStarLine() {
        return firstOf(UlLine(), StarLine());
    }

    Rule StarLine() {
        return firstOf(NOrMore('*', 4), sequence(Spacechar(), oneOrMore('*'), test(Spacechar())));
    }

    Rule UlLine() {
        return firstOf(NOrMore('_', 4), sequence(Spacechar(), oneOrMore('_'), test(Spacechar())));
    }

    Rule Emph() {
        return firstOf(EmphStar(), EmphUl());
    }

    Rule EmphStar() {
        return sequence(OneStarOpen(), zeroOrMore(sequence(testNot(OneStarClose()), Inline())), OneStarClose());
    }

    Rule OneStarOpen() {
        return sequence(testNot(StarLine()), '*', testNot(Spacechar()), testNot(Newline()));
    }

    Rule OneStarClose() {
        return sequence(testNot(Spacechar()), testNot(Newline()), Inline(), testNot(StrongStar()), '*');
    }

    Rule EmphUl() {
        return sequence(OneUlOpen(), zeroOrMore(sequence(testNot(OneUlClose()), Inline())), OneUlClose());
    }

    Rule OneUlOpen() {
        return sequence(testNot(UlLine()), '_', testNot(Spacechar()), testNot(Newline()));
    }

    Rule OneUlClose() {
        return sequence(testNot(Spacechar()), testNot(Newline()), Inline(), testNot(StrongUl()), '_',
                testNot(Alphanumeric()));
    }

    Rule Strong() {
        return firstOf(StrongStar(), StrongUl());
    }

    Rule StrongStar() {
        return sequence(TwoStarOpen(), zeroOrMore(sequence(testNot(TwoStarClose()), Inline())), TwoStarClose());
    }

    Rule TwoStarOpen() {
        return sequence(testNot(StarLine()), "**", testNot(Spacechar()), testNot(Newline()));
    }

    Rule TwoStarClose() {
        return sequence(testNot(Spacechar()), testNot(Newline()), Inline(), "**");
    }

    Rule StrongUl() {
        return sequence(TwoUlOpen(), zeroOrMore(sequence(testNot(TwoUlClose()), Inline())), TwoUlClose());
    }

    Rule TwoUlOpen() {
        return sequence(testNot(UlLine()), "__", testNot(Spacechar()), testNot(Newline()));
    }

    Rule TwoUlClose() {
        return sequence(testNot(Spacechar()), testNot(Newline()), Inline(), "__", testNot(Alphanumeric()));
    }

    //************* LINKS ****************

    Rule Image() {
        return sequence('!', firstOf(ExplicitLink(), ReferenceLink()));
    }

    Rule Link() {
        return firstOf(ExplicitLink(), ReferenceLink(), AutoLinkUrl(), AutoLinkEmail());
    }

    Rule ExplicitLink() {
        return sequence(Label(), Spn1(), '(', Sp(), Source(), Spn1(), optional(Title()), Sp(), ')');
    }

    Rule ReferenceLink() {
        return firstOf(ReferenceLinkSingle(), ReferenceLinkDouble());
    }

    Rule ReferenceLinkSingle() {
        return sequence(Label(), optional(sequence(Spn1(), "[]")));
    }

    Rule ReferenceLinkDouble() {
        return sequence(Label(), Spn1(), testNot("[]"), Label());
    }

    Rule Source() {
        return firstOf(sequence('<', SourceContents(), '>'), SourceContents());
    }

    Rule SourceContents() {
        return oneOrMore(firstOf(
                oneOrMore(sequence(testNot('('), testNot(')'), testNot('>'), Nonspacechar())),
                sequence('(', SourceContents(), ')')
        ));
    }

    Rule Title() {
        return firstOf(TitleSingle(), TitleDouble());
    }

    Rule TitleSingle() {
        return sequence('\'', zeroOrMore(sequence(
                testNot(sequence('\'', Sp(), firstOf(')', Newline()))), testNot(Newline()), any()
        )), '\'');
    }

    Rule TitleDouble() {
        return sequence('"', zeroOrMore(sequence(
                testNot(sequence('"', Sp(), firstOf(')', Newline()))), testNot(Newline()), any()
        )), '"');
    }

    Rule AutoLinkUrl() {
        return sequence('<', oneOrMore(Letter()), "://",
                oneOrMore(sequence(testNot(Newline()), testNot('>'), any())), '>');
    }

    Rule AutoLinkEmail() {
        return sequence('<', oneOrMore(firstOf(Alphanumeric(), charSet("-+_"))), '@',
                oneOrMore(sequence(testNot(Newline()), testNot('>'), any())), '>');
    }

    //************* REFERENCE ****************

    Rule Reference() {
        return sequence(NonindentSpace(), testNot("[]"), Label(), ':', Spn1(), RefSrc(), Spn1(), optional(RefTitle()),
                zeroOrMore(BlankLine()));
    }

    Rule Label() {
        return sequence('[', zeroOrMore(sequence(testNot(']'), Inline())), ']');
    }

    Rule RefSrc() {
        return oneOrMore(Nonspacechar());
    }

    Rule RefTitle() {
        return firstOf(RefTitleSingle(), RefTitleDouble(), RefTitleParens());
    }

    Rule RefTitleSingle() {
        return sequence('\'', zeroOrMore(
                sequence(testNot(firstOf(sequence('\'', Sp(), Newline()), Newline())), any())
        ), '\'');
    }

    Rule RefTitleDouble() {
        return sequence('"', zeroOrMore(
                sequence(testNot(firstOf(sequence('"', Sp(), Newline()), Newline())), any())
        ), '"');
    }

    Rule RefTitleParens() {
        return sequence('(', zeroOrMore(
                sequence(testNot(firstOf(sequence(')', Sp(), Newline()), Newline())), any())
        ), ')');
    }

    //************* CODE ****************

    Rule Code() {
        return firstOf(
                sequence(Ticks1(), Sp(), oneOrMore(firstOf(
                        sequence(testNot('`'), Nonspacechar()),
                        sequence(testNot(Ticks1()), oneOrMore('`')),
                        sequence(testNot(sequence(Sp(), Ticks1())),
                                firstOf(Spacechar(), sequence(Newline(), testNot(BlankLine()))))
                )), Sp(), Ticks1()),
                sequence(Ticks2(), Sp(), oneOrMore(firstOf(
                        sequence(testNot('`'), Nonspacechar()),
                        sequence(testNot(Ticks2()), oneOrMore('`')),
                        sequence(testNot(sequence(Sp(), Ticks2())),
                                firstOf(Spacechar(), sequence(Newline(), testNot(BlankLine()))))
                )), Sp(), Ticks2()),
                sequence(Ticks3(), Sp(), oneOrMore(firstOf(
                        sequence(testNot('`'), Nonspacechar()),
                        sequence(testNot(Ticks3()), oneOrMore('`')),
                        sequence(testNot(sequence(Sp(), Ticks3())),
                                firstOf(Spacechar(), sequence(Newline(), testNot(BlankLine()))))
                )), Sp(), Ticks3()),
                sequence(Ticks4(), Sp(), oneOrMore(firstOf(
                        sequence(testNot('`'), Nonspacechar()),
                        sequence(testNot(Ticks4()), oneOrMore('`')),
                        sequence(testNot(sequence(Sp(), Ticks4())),
                                firstOf(Spacechar(), sequence(Newline(), testNot(BlankLine()))))
                )), Sp(), Ticks4()),
                sequence(Ticks5(), Sp(), oneOrMore(firstOf(
                        sequence(testNot('`'), Nonspacechar()),
                        sequence(testNot(Ticks5()), oneOrMore('`')),
                        sequence(testNot(sequence(Sp(), Ticks5())),
                                firstOf(Spacechar(), sequence(Newline(), testNot(BlankLine()))))
                )), Sp(), Ticks5())
        );
    }

    Rule Ticks1() {
        return sequence('`', testNot('`'));
    }

    Rule Ticks2() {
        return sequence("``", testNot('`'));
    }

    Rule Ticks3() {
        return sequence("```", testNot('`'));
    }

    Rule Ticks4() {
        return sequence("````", testNot('`'));
    }

    Rule Ticks5() {
        return sequence("`````", testNot('`'));
    }

    //************* RAW HTML ****************

    Rule RawHtml() {
        return firstOf(HtmlComment(), HtmlTag());
    }

    Rule HtmlComment() {
        return sequence("<!--", zeroOrMore(sequence(testNot("-->"), any())), "-->");
    }

    Rule HtmlTag() {
        return sequence('<', Spn1(), optional('/'), oneOrMore(Alphanumeric()), Spn1(), zeroOrMore(HtmlAttribute()),
                optional('/'), Spn1(), '>');
    }

    Rule HtmlAttribute() {
        return sequence(
                zeroOrMore(firstOf(Alphanumeric(), '-')),
                Spn1(),
                optional(sequence('=', Spn1(), firstOf(Quoted(), oneOrMore(sequence(testNot('>'), Nonspacechar()))))),
                Spn1()
        );
    }

    Rule Quoted() {
        return firstOf(
                sequence('"', zeroOrMore(sequence(testNot('"'), any())), '"'),
                sequence('\'', zeroOrMore(sequence(testNot('\''), any())), '\'')
        );
    }

    //************* LINES ****************

    Rule NonblankIndentedLine() {
        return sequence(testNot(BlankLine()), IndentedLine());
    }

    Rule BlankLine() {
        return sequence(Sp(), Newline());
    }

    Rule IndentedLine() {
        return sequence(Indent(), Line());
    }

    Rule OptionallyIndentedLine() {
        return sequence(optional(Indent()), Line());
    }

    Rule Line() {
        return firstOf(
                sequence(zeroOrMore(sequence(testNot('\r'), testNot('\n'), any())), Newline()),
                sequence(oneOrMore(any()), eoi())
        );
    }

    //************* QUOTES ****************

    Rule SingleQuoted() {
        return sequence(SingleQuoteStart(), oneOrMore(sequence(testNot(SingleQuoteEnd()), Inline())), SingleQuoteEnd());
    }

    Rule SingleQuoteStart() {
        return sequence('\'', testNot(charSet(")!],.;:-? \t\n")),
                testNot(sequence(firstOf('s', 't', "m", "ve", "ll", "re"), testNot(Alphanumeric()))));
    }

    Rule SingleQuoteEnd() {
        return sequence('\'', testNot(Alphanumeric()));
    }

    Rule DoubleQuoted() {
        return sequence('"', oneOrMore(sequence(testNot('"'), Inline())), '"');
    }

    //************* NOTES ****************

    Rule InlineNote() {
        return sequence("^[", oneOrMore(sequence(testNot(']'), Inline())), ']');
    }

    Rule NoteReference() {
        return RawNoteReference();
    }

    Rule RawNoteReference() {
        return sequence("[^", oneOrMore(sequence(testNot(Newline()), testNot(']'), any())), ']');
    }

    Rule Note() {
        return sequence(NonindentSpace(), RawNoteReference(), ':', Sp(), RawNoteBlock(),
                zeroOrMore(sequence(test(Indent()), RawNoteBlock())));
    }

    Rule RawNoteBlock() {
        return sequence(oneOrMore(sequence(testNot(BlankLine()), OptionallyIndentedLine())), zeroOrMore(BlankLine()));
    }

    //************* ENTITIES ****************

    Rule Entity() {
        return firstOf(HexEntity(), DecEntity(), CharEntity());
    }

    Rule HexEntity() {
        return sequence("&", charSet("xX"), oneOrMore(firstOf(Digit(), charRange('a', 'f'), charRange('A', 'F'))), ';');
    }

    Rule DecEntity() {
        return sequence("&#", oneOrMore(Digit()), ';');
    }

    Rule CharEntity() {
        return sequence('&', oneOrMore(Alphanumeric()), ';');
    }

    //************* BASICS ****************

    Rule Str() {
        return sequence(NormalChar(), zeroOrMore(sequence(zeroOrMore('_'), NormalChar())));
    }

    Rule Space() {
        return oneOrMore(Spacechar());
    }

    Rule Spn1() {
        return sequence(Sp(), optional(sequence(Newline(), Sp())));
    }

    Rule Sp() {
        return zeroOrMore(Spacechar());
    }

    Rule Spacechar() {
        return charSet(" \t");
    }

    Rule Nonspacechar() {
        return sequence(testNot(Spacechar()), testNot(Newline()), any());
    }

    Rule NormalChar() {
        return sequence(testNot(firstOf(SpecialChar(), Spacechar(), Newline())), any());
    }

    Rule EscapedChar() {
        return sequence('\\', testNot(Newline()), any());
    }

    Rule SpecialChar() {
        return charSet("*_`&[]<!\\.-'\"^");
    }

    Rule Newline() {
        return firstOf('\n', sequence('\r', optional('\n')));
    }

    Rule NonindentSpace() {
        return firstOf("   ", "  ", " ", empty());
    }

    Rule Indent() {
        return firstOf('\t', "    ");
    }

    Rule Apostrophe() {
        return ch('\'');
    }

    Rule Ellipsis() {
        return firstOf("...", ". . .");
    }

    Rule EnDash() {
        return sequence('-', test(Digit()));
    }

    Rule EmDash() {
        return firstOf("---", "--");
    }

    Rule Alphanumeric() {
        return firstOf(Letter(), Digit());
    }

    Rule Letter() {
        return firstOf(charRange('A', 'Z'), charRange('a', 'z'));
    }

    Rule Digit() {
        return charRange('0', '9');
    }

    //************* HELPERS ****************

    Rule NOrMore(char c, int n) {
        return sequence(StringUtils.repeat(c, n), zeroOrMore(c));
    }

}
