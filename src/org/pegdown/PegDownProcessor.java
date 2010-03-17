package org.pegdown;

import org.jetbrains.annotations.NotNull;
import org.parboiled.Parboiled;
import org.parboiled.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class PegDownProcessor {

    private final PegDownParser parser;

    public PegDownProcessor() {
        this(false);
    }

    public PegDownProcessor(boolean suppressHtml) {
        parser = Parboiled.createParser(PegDownParser.class, suppressHtml);
    }

    public String toHtml(@NotNull String markDownSource) {
        ParsingResult<AstNode> result = ReportingParseRunner.run(parser.Doc(), markDownSource);
        if (!result.matched) {
            String errorMessage = "Internal error";
            if (result.hasErrors()) errorMessage += ": " + result.parseErrors.get(0);
            throw new RuntimeException(errorMessage);
        }

        return ParseTreeUtils.printNodeTree(result);
    }

}
