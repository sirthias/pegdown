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
import org.pegdown.ast.RootNode;

/**
 * A clean and lightweight Markdown-to-HTML filter based on a PEG parser implemented with parboiled.
 * Note: A PegDownProcessor is not thread-safe (since it internally reused the parboiled parser instance).
 * If you need to process markdown source in parallel create one PegDownProcessor per thread!
 *
 * @see <a href="http://daringfireball.net/projects/markdown/">Markdown</a>
 * @see <a href="http://www.parboiled.org/">parboiled.org</a>
 */
public class PegDownProcessor {
    private final int tabstop;
    private final Parser parser;

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
    public PegDownProcessor(int options) {
        this(options, 4);
    }

    /**
     * Creates a new processor instance with the given {@link org.pegdown.Extensions}.
     *
     * @param options the flags of the extensions to enable as a bitmask
     * @param tabstop the number of spaces in a tab
     */
    public PegDownProcessor(int options, int tabstop) {
        this(Parboiled.createParser(Parser.class, options), tabstop);
    }

    /**
     * Creates a new processor instance using the given Parser and tabstop width.
     *
     * @param parser the parser instance to use
     * @param tabstop the number of spaces in a tab 
     */
    public PegDownProcessor(Parser parser, int tabstop) {
        this.parser = parser;
        this.tabstop = tabstop;
    }

    /**
     * Converts the given markdown source to HTML.
     *
     * @param markdownSource the markdown source to convert
     * @return the HTML
     */
    public String markdownToHtml(String markdownSource) {
        return markdownToHtml(markdownSource.toCharArray());
    }

    /**
     * Converts the given markdown source to HTML.
     *
     * @param markdownSource the markdown source to convert
     * @return the HTML
     */
    public String markdownToHtml(char[] markdownSource) {
        RootNode astRoot = parseMarkdown(markdownSource);
        return new ToHtmlSerializer().toHtml(astRoot);
    }

    /**
     * Parses the given markdown source and returns the root node of the generated Abstract Syntax Tree.
     *
     * @param markdownSource the markdown source to convert
     * @return the AST root
     */
    public RootNode parseMarkdown(char[] markdownSource) {
        return parser.parse(prepareSource(markdownSource));
    }

    /**
     * Performs tabstop expansion and adds two trailing newlines.
     *
     * @param markDownSource the markdown source to process
     * @return the processed source
     */
    public char[] prepareSource(char[] markDownSource) {
        StringBuilder sb = new StringBuilder(markDownSource.length + 2);
        int charsToTab = tabstop;
        for (char c : markDownSource) {
            switch (c) {
                case '\t':
                    while (charsToTab > 0) {
                        sb.append(' ');
                        charsToTab--;
                    }
                    break;
                case '\n':
                    sb.append('\n');
                    charsToTab = tabstop;
                    break;
                default:
                    sb.append(c);
                    charsToTab--;
            }
            if (charsToTab == 0) charsToTab = tabstop;
        }
        sb.append('\n');
        sb.append('\n');
        char[] buf = new char[sb.length()];
        sb.getChars(0, buf.length, buf, 0);
        return buf;
    }
}
