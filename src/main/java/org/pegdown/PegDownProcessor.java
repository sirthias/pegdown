/*
 * Copyright (C) 2010-2011 Mathias Doenitz
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
    public final Parser parser;

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
        this(Parboiled.createParser(Parser.class, options));
    }

    /**
     * Creates a new processor instance using the given Parser and tabstop width.
     *
     * @param parser the parser instance to use
     */
    public PegDownProcessor(Parser parser) {
        this.parser = parser;
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
     * @param noFollow the NoFollow to use
     * @return the HTML
     */
    public String markdownToHtml(String markdownSource, NoFollow noFollow) {
        return markdownToHtml(markdownSource.toCharArray(), noFollow);
    }

    /**
     * Converts the given markdown source to HTML.
     *
     * @param markdownSource the markdown source to convert
     * @return the HTML
     */
    public String markdownToHtml(char[] markdownSource) {
        return markdownToHtml(markdownSource, NoFollow.NEVER);
    }

    /**
     * Converts the given markdown source to HTML.
     *
     * @param markdownSource the markdown source to convert
     * @param noFollow the NoFollow to use
     * @return the HTML
     */
    public String markdownToHtml(char[] markdownSource, NoFollow noFollow) {
        RootNode astRoot = parseMarkdown(markdownSource);
        return new ToHtmlSerializer(noFollow).toHtml(astRoot);
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
     * Adds two trailing newlines.
     *
     * @param source the markdown source to process
     * @return the processed source
     */
    public char[] prepareSource(char[] source) {
        char[] src = new char[source.length + 2];
        System.arraycopy(source, 0, src, 0, source.length);
        src[source.length] = '\n';
        src[source.length + 1] = '\n';
        return src;
    }
}
