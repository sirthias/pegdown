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

import org.parboiled.Parboiled;
import org.parboiled.support.ParsingResult;
import org.pegdown.ast.Node;

import static org.parboiled.errors.ErrorUtils.printParseErrors;

/**
 * A clean and lightweight Markdown-to-HTML filter based on a PEG parser implemented with parboiled.
 *
 * @see <a href="http://daringfireball.net/projects/markdown/">Markdown</a>
 * @see <a href="http://www.parboiled.org/">parboiled.org</a>
 */
public class PegDownProcessor {

    /**
     * Defines the number of spaces in a tab, can be changed externally if required.
     */
    public static int TABSTOP = 4;

    private final Parser parser;
    private ParsingResult<Node> lastParsingResult;

    /**
     * Creates a new processor instance without any enabled extensions.
     */
    public PegDownProcessor() {
        this(Extensions.NONE);
    }

    /**
     * Creates a new processor instance with the given {@link org.pegdown.Extensions}.
     *
     * @param options the flags of the extensions to enable as a bitmask
     */
    @SuppressWarnings({"unchecked"})
    public PegDownProcessor(int options) {
        parser = Parboiled.createParser(Parser.class, options);
    }

    /**
     * Returns the underlying parboiled parser object
     *
     * @return the parser
     */
    Parser getParser() {
        return parser;
    }

    ParsingResult<Node> getLastParsingResult() {
        return lastParsingResult;
    }

    /**
     * Converts the given markdown source to HTML.
     *
     * @param markdownSource the markdown source to convert
     * @return the HTML
     */
    public String markdownToHtml(String markdownSource) {
        parser.references.clear();
        parser.abbreviations.clear();

        lastParsingResult = parser.parseRawBlock(prepare(markdownSource));
        if (lastParsingResult.hasErrors()) {
            throw new RuntimeException("Internal error during markdown parsing:\n--- ParseErrors ---\n" +
                    printParseErrors(lastParsingResult)/* +
                    "\n--- ParseTree ---\n" +
                    printNodeTree(result)*/
            );
        }

        Printer htmlVisitor = new Printer(parser.references, parser.abbreviations);

        htmlVisitor.visit(lastParsingResult.resultValue);

        return htmlVisitor.getString();
    }

    // perform tabstop expansion and add two trailing newlines

    static String prepare(String markDownSource) {
        StringBuilder sb = new StringBuilder(markDownSource.length() + 2);
        int charsToTab = TABSTOP;
        for (int i = 0; i < markDownSource.length(); i++) {
            char c = markDownSource.charAt(i);
            switch (c) {
                case '\t':
                    while (charsToTab > 0) {
                        sb.append(' ');
                        charsToTab--;
                    }
                    break;
                case '\n':
                    sb.append('\n');
                    charsToTab = TABSTOP;
                    break;
                default:
                    sb.append(c);
                    charsToTab--;
            }
            if (charsToTab == 0) charsToTab = TABSTOP;
        }
        sb.append('\n');
        sb.append('\n');
        return sb.toString();
    }

}
