package org.pegdown;

import org.parboiled.Rule;
import org.parboiled.common.ArrayBuilder;

/**
 * Parboiled parser for the extended pegdown Markdown syntax.
 * Builds an Abstract Syntax Tree (AST) of {@link org.pegdown.AstNode} objects.
 */
@SuppressWarnings({"InfiniteRecursion"})
public class ExtendedMarkDownParser extends MarkDownParser implements Extensions {

    private final int options;

    public ExtendedMarkDownParser(Integer options) {
        this.options = options;
    }

    @Override
    Rule Inline() {
        ArrayBuilder<Rule> rules = new ArrayBuilder<Rule>(
                Str(), Endline(), UlOrStarLine(), Space(), Strong(), Emph(), Image(), Link(), Code(), RawHtml(),
                Entity(), EscapedChar()
        );
        if (ext(QUOTES)) {
            rules.add(SingleQuoted(), DoubleQuoted());
        }
        if (ext(SMARTS)) {
            rules.add(Ellipsis(), EnDash(), EmDash(), Apostrophe());
        }
        return FirstOf(rules.add(Symbol()).get());
    }

    @Override
    Rule SpecialChar() {
        String chars = "*_`&[]<!\\";
        if (ext(QUOTES)) {
            chars += "'\"";
        }
        if (ext(SMARTS)) {
            chars += ".-";
        }
        return CharSet(chars);
    }

    //************* SMARTS ****************

    Rule Apostrophe() {
        return Sequence('\'', set(new AstNode(APOSTROPHE)));
    }

    Rule Ellipsis() {
        return Sequence(FirstOf("...", ". . ."), set(new AstNode(ELLIPSIS)));
    }

    Rule EnDash() {
        return Sequence('-', Test(Digit()), set(new AstNode(ENDASH)));
    }

    Rule EmDash() {
        return Sequence(FirstOf("---", "--"), set(new AstNode(EMDASH)));
    }

    //************* QUOTES ****************

    Rule SingleQuoted() {
        return Sequence(
                SingleQuoteStart(),
                set(new AstNode(SINGLE_QUOTED)),
                OneOrMore(Sequence(TestNot(SingleQuoteEnd()), Inline(), UP2(value().addChild(prevValue())))),
                SingleQuoteEnd()
        );
    }

    Rule SingleQuoteStart() {
        return Sequence('\'', TestNot(CharSet(")!],.;:-? \t\n")),
                TestNot(Sequence(FirstOf('s', 't', "m", "ve", "ll", "re"), TestNot(Alphanumeric()))));
    }

    Rule SingleQuoteEnd() {
        return Sequence('\'', TestNot(Alphanumeric()));
    }

    Rule DoubleQuoted() {
        return Sequence(
                '"',
                set(new AstNode(DOUBLE_QUOTED)),
                OneOrMore(Sequence(TestNot('"'), Inline(), UP2(value().addChild(prevValue())))),
                '"'
        );
    }

    //************* HELPERS ****************

    private boolean ext(int extension) {
        return (options & extension) > 0;
    }

}