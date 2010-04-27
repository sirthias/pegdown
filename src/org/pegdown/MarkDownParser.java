package org.pegdown;

import org.parboiled.*;
import org.parboiled.annotations.Cached;
import org.parboiled.annotations.Label;
import org.parboiled.annotations.SkipActionsInPredicates;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.common.StringUtils;
import org.parboiled.google.base.Function;
import org.parboiled.support.ParsingResult;

import java.util.ArrayList;
import java.util.List;

import static org.parboiled.trees.TreeUtils.addChild;

@SuppressWarnings({"InfiniteRecursion"})
@SkipActionsInPredicates
public class MarkDownParser extends BaseParser<AstNode> implements AstNodeType {

    static final String[] HTML_TAGS = new String[]{"address", "blockquote", "center", "dir", "div", "dl",
            "fieldset", "form", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "isindex", "menu", "noframes", "noscript",
            "ol", "p", "pre", "table", "ul", "dd", "dt", "frameset", "li", "tbody", "td", "tfoot", "th", "thead", "tr",
            "script", "style"};

    // special list for quick access to all AstNodes for link references in the document
    // with this list we do not need to search the AST for reference nodes but already have them right here
    final List<AstNode> references = new ArrayList<AstNode>();

    Rule Doc() {
        return Sequence(
                set(new AstNode(DEFAULT)),
                ZeroOrMore(
                        Sequence(Block(), UP2(value().addChild(prevValue())))
                )
        );
    }

    Rule Block() {
        return Sequence(
                ZeroOrMore(BlankLine()),
                FirstOf(BlockQuote(), Verbatim(), Note(), Reference(), HorizontalRule(), Heading(), OrderedList(),
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
                                '>', Optional(' '), Line(), innerSource.append(prevValue().text),
                                ZeroOrMore(
                                        Sequence(
                                                TestNot('>'),
                                                TestNot(BlankLine()),
                                                Line(), innerSource.append(prevValue().text)
                                        )
                                ),
                                ZeroOrMore(
                                        Sequence(BlankLine(), innerSource.append("\n"))
                                )
                        )
                ),
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
                                NonblankIndentedLine(), text.append(temp.getAndSet(""), prevValue().text)
                        )
                ),
                set(new AstNode(VERBATIM, text.get()))
        );
    }

    @SuppressSubnodes
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
                OneOrMore(
                        Sequence(SetextInline(), UP2(value().addChild(prevValue())))
                ),
                Newline(), NOrMore('=', 3), Newline()
        );
    }

    Rule SetextHeading2() {
        return Sequence(
                SetextInline(), set(new AstNode(H2).withChild(prevValue())),
                OneOrMore(
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
                ListItem(TIGHT_LIST_ITEM), set(new AstNode(DEFAULT).withChild(prevValue())),
                ZeroOrMore(
                        Sequence(ListItem(TIGHT_LIST_ITEM), UP2(value().addChild(prevValue())))
                ),
                ZeroOrMore(BlankLine()),
                TestNot(FirstOf(Bullet(), Enumerator()))
        );
    }

    Rule ListLoose() {
        return Sequence(
                ListItem(LOOSE_LIST_ITEM), set(new AstNode(DEFAULT).withChild(prevValue())),
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
                innerSource.set(prevValue().text),

                ZeroOrMore(
                        Sequence(
                                FirstOf(
                                        // if we have blank lines append them to the inner source
                                        OneOrMore(Sequence(BlankLine(), blanks.append("\n"))),

                                        // if we do not have a blank line we need to trigger an inner parse
                                        // so store the current inner source value in the helper string
                                        // and reinitialize the inner source
                                        blanks.set("\u0001")
                                ),
                                OneOrMore(
                                        Sequence(
                                                Indent(), ListBlock(),

                                                // append potentially captured blanks and the block text
                                                innerSource.append(blanks.getAndSet(""), prevValue().text)
                                        )
                                ),
                                extraNLs.set("\n\n")
                        )
                ),

                // finally, after having built the complete source we run an inner parse and attach its AST root
                listAction(type, innerSource.get() + extraNLs.get())
        );
    }

    boolean listAction(int type, String innerSource) {
        int start = 0;
        int end = innerSource.indexOf('\u0001', start);
        if (end == -1) {
            Context<AstNode> context = getContext();
            AstNode astNode = parseRawBlock(type == LOOSE_LIST_ITEM ? innerSource + "\n\n" : innerSource).parseTreeRoot
                    .getValue();
            setContext(context);
            return set(astNode.withType(type));
        }

        create(type);

        while (true) {
            end = innerSource.indexOf('\u0001', start);
            if (end == -1) end = innerSource.length();
            String sourcePart = innerSource.substring(start, end);

            Context<AstNode> context = getContext();
            AstNode astNode = parseRawBlock(type == LOOSE_LIST_ITEM ? sourcePart + "\n\n" : sourcePart).parseTreeRoot
                    .getValue();
            setContext(context);
            attach(astNode.getChildren().get(0));

            if (end == innerSource.length()) return true;
            start = end + 1;
        }
    }

    Rule ListBlock() {
        Var<String> source = new Var<String>();
        return Sequence(
                Line(),
                source.set(prevValue().text),
                ZeroOrMore(
                        Sequence(ListBlockLine(), source.set(source.get() + prevValue().text))
                ),
                create(DEFAULT, source.get())
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
        Var<String> blockSource = new Var<String>();
        return Sequence(
                Sequence(
                        FirstOf(HtmlBlockInTags(), HtmlComment(), HtmlBlockSelfClosing()), blockSource.set(prevText()),
                        OneOrMore(BlankLine())
                ),
                create(HTMLBLOCK, blockSource.get())
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
        return Sequence('<', Spn1(), HtmlBlockType(), Spn1(), ZeroOrMore(HtmlAttribute()), Optional('/'), Spn1(), '>');
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
                inPredicate() || set(lastValue().withType(EMPH))
        );
    }

    Rule Strong() {
        return Sequence(
                FirstOf(EmphOrStrong("**"), EmphOrStrong("__")),
                inPredicate() || set(lastValue().withType(STRONG))
        );
    }

    @Label
    Rule EmphOrStrong(String chars) {
        return Sequence(
                EmphOrStrongOpen(chars),
                inPredicate() || create(DEFAULT),
                ZeroOrMore(
                        Sequence(
                                TestNot(EmphOrStrongClose(chars)),
                                Inline(), inPredicate() || emAction(getContext(), lastValue())
                        )
                ),
                inPredicate() || debug(getContext()),
                EmphOrStrongClose(chars), inPredicate() || attach(lastValue())
        );
    }

    boolean debug(Context<AstNode> context) {
        return true;
    }

    boolean emAction(Context<AstNode> context, AstNode astNode) {
        setContext(getContext().getParent().getParent());
        return attach(astNode);
    }

    @Label
    Rule EmphOrStrongOpen(String chars) {
        return Sequence(
                TestNot(CharLine(chars.charAt(0))),
                chars,
                TestNot(Spacechar()),
                TestNot(Newline())
        );
    }

    @Cached
    @Label
    Rule EmphOrStrongClose(String chars) {
        return Sequence(
                TestNot(Spacechar()),
                TestNot(Newline()),
                Inline(), set(),
                chars.length() == 1 ? TestNot(EmphOrStrong(chars + chars)) : Empty(),
                chars,
                chars.charAt(0) == '_' ? TestNot(Alphanumeric()) : Empty()
        );
    }

    //************* LINKS ****************

    Rule Image() {
        return Sequence('!',
                FirstOf(
                        Sequence(ExplicitLink(), set(lastValue().withType(EXP_IMG_LINK))),
                        Sequence(ReferenceLink(), set(lastValue().withType(REF_IMG_LINK)))
                )
        );
    }

    Rule Link() {
        return FirstOf(ExplicitLink(), ReferenceLink(), AutoLinkUrl(), AutoLinkEmail());
    }

    Rule ExplicitLink() {
        return Sequence(
                Label(), create(EXP_LINK, lastValue()),
                Spn1(), '(', Sp(),
                Source(), attach(),
                Spn1(), Optional(Sequence(Title(), UP2(attach()))),
                Sp(), ')'
        );
    }

    Rule ReferenceLink() {
        Var<String> spaceSource = new Var<String>();
        return Sequence(
                Label(), create(REF_LINK, lastValue()),
                FirstOf(
                        // regular reference link
                        Sequence(Spn1(), spaceSource.set(prevText()), TestNot("[]"), Label(),
                                set(lastValue().withType(LINK_REF))),

                        // implicit reference link
                        Optional(Sequence(Spn1(), spaceSource.set(prevText()), "[]", create(LINK_REF, "")))
                ),
                attach(new AstNode(SPACE, spaceSource.get())),
                attach()
        );
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
                Sequence('(', SourceContents(), ')'),
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
                create(LINK_TITLE, lastText()),
                delimiter
        );
    }

    Rule AutoLinkUrl() {
        return Sequence(
                '<',
                Sequence(OneOrMore(Letter()), "://", OneOrMore(Sequence(TestNot(Newline()), TestNot('>'), Any()))),
                create(AUTO_LINK, lastText()),
                '>'
        );
    }

    Rule AutoLinkEmail() {
        return Sequence(
                '<',
                Sequence(OneOrMore(FirstOf(Alphanumeric(), CharSet("-+_"))), '@',
                        OneOrMore(Sequence(TestNot(Newline()), TestNot('>'), Any()))),
                create(MAIL_LINK, lastText()),
                '>'
        );
    }

    //************* REFERENCE ****************

    Rule Reference() {
        return Sequence(
                NonindentSpace(), TestNot("[]"), Label(), create(REFERENCE, lastValue()),
                ':', Spn1(), RefSrc(), attach(),
                Spn1(), Optional(Sequence(RefTitle(), UP2(attach()))),
                ZeroOrMore(BlankLine()),
                references.add(value())
        );
    }

    Rule Label() {
        return Sequence(
                '[', create(LINK_LABEL),
                ZeroOrMore(Sequence(TestNot(']'), Inline(), UP2(attach(lastValue())))),
                ']'
        );
    }

    Rule RefSrc() {
        return Sequence(OneOrMore(Nonspacechar()), create(LINK_URL, lastText()));
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
                create(CODE, prevText()),
                Sp(), ticks
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

    @SuppressSubnodes
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
        Var<String> line = new Var<String>();
        return Sequence(
                FirstOf(
                        Sequence(
                                ZeroOrMore(Sequence(TestNot('\r'), TestNot('\n'), Any())), line.set(prevText() + '\n'),
                                Newline()
                        ),
                        Sequence(OneOrMore(Any()), line.set(prevText()), Eoi())
                ),
                create(DEFAULT, line.get())
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
                Sequence(NormalChar(), ZeroOrMore(Sequence(ZeroOrMore('_'), NormalChar()))).suppressSubnodes(),
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

    boolean attach() {
        return attach(lastValue());
    }

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
        return set(new AstNode(type, text)) && value().addChild(child);
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
