package org.pegdown;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.support.StringBuilderVar;
import org.pegdown.plugins.BlockPluginParser;
import org.pegdown.plugins.InlinePluginParser;

public class PluginParser extends Parser implements InlinePluginParser, BlockPluginParser {

    public PluginParser() {
        super(ALL, 1000l, DefaultParseRunnerProvider);
    }

    @Override
    public Rule[] blockPluginRules() {
        return new Rule[] {BlockPlugin()};
    }

    @Override
    public Rule[] inlinePluginRules() {
        return new Rule[] {InlinePlugin()};
    }

    public Rule InlinePlugin() {
        StringBuilderVar text = new StringBuilderVar();
        return NodeSequence(
                Ch('%'),
                OneOrMore(TestNot(Ch('%')), BaseParser.ANY, text.append(matchedChar())),
                push(new InlinePluginNode(text.getString())),
                Ch('%')
        );
    }

    public Rule BlockPlugin() {
        StringBuilderVar text = new StringBuilderVar();
        return NodeSequence(
                BlockPluginMarker(),
                OneOrMore(TestNot(Newline(), BlockPluginMarker()), BaseParser.ANY, text.append(matchedChar())),
                Newline(),
                push(new BlockPluginNode(text.appended('\n').getString())),
                BlockPluginMarker()
        );
    }

    public Rule BlockPluginMarker() {
        return Sequence(NOrMore('%', 3), Newline());
    }
}
