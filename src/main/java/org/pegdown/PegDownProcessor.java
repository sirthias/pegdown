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

import java.util.Collections;
import java.util.Map;

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
    public static final long DEFAULT_MAX_PARSING_TIME = 2000;

    public final Parser parser;

    /**
     * Creates a new processor instance without any enabled extensions and the default parsing timeout.
     */
    public PegDownProcessor() {
        this(DEFAULT_MAX_PARSING_TIME);
    }

    /**
     * Creates a new processor instance without any enabled extensions and the given parsing timeout.
     */
    public PegDownProcessor(long maxParsingTimeInMillis) {
        this(Extensions.NONE, maxParsingTimeInMillis);
    }

    /**
     * Creates a new processor instance with the given {@link org.pegdown.Extensions} and the default parsing timeout.
     *
     * @param options the flags of the extensions to enable as a bitmask
     */
    public PegDownProcessor(int options) {
        this(options, DEFAULT_MAX_PARSING_TIME);
    }

    /**
     * Creates a new processor instance with the given {@link org.pegdown.Extensions} and parsing timeout.
     *
     * @param options the flags of the extensions to enable as a bitmask
     */
    public PegDownProcessor(int options, long maxParsingTimeInMillis) {
        this(Parboiled.createParser(Parser.class, options, maxParsingTimeInMillis, Parser.DefaultParseRunnerProvider));
    }

    /**
     * Creates a new processor instance using the given Parser.
     *
     * @param parser the parser instance to use
     */
    public PegDownProcessor(Parser parser) {
        this.parser = parser;
    }

    /**
     * Converts the given markdown source to HTML.
     * If the input cannot be parsed within the configured parsing timeout the method returns null.
     *
     * @param markdownSource the markdown source to convert
     * @return the HTML
     */
    public String markdownToHtml(String markdownSource) {
        return markdownToHtml(markdownSource.toCharArray());
    }

    /**
     * Converts the given markdown source to HTML.
     * If the input cannot be parsed within the configured parsing timeout the method returns null.
     *
     * @param markdownSource the markdown source to convert
     * @param linkRenderer the LinkRenderer to use
     * @return the HTML
     */
    public String markdownToHtml(String markdownSource, LinkRenderer linkRenderer) {
        return markdownToHtml(markdownSource.toCharArray(), linkRenderer);
    }

	public String markdownToHtml(String markdownSource, LinkRenderer linkRenderer, Map<String, VerbatimSerializer> verbatimSerializerMap) {
		return markdownToHtml(markdownSource.toCharArray(), linkRenderer, verbatimSerializerMap);
	}

    /**
     * Converts the given markdown source to HTML.
     * If the input cannot be parsed within the configured parsing timeout the method returns null.
     *
     * @param markdownSource the markdown source to convert
     * @return the HTML
     */
    public String markdownToHtml(char[] markdownSource) {
        return markdownToHtml(markdownSource, new LinkRenderer());
    }

    /**
     * Converts the given markdown source to HTML.
     * If the input cannot be parsed within the configured parsing timeout the method returns null.
     *
     * @param markdownSource the markdown source to convert
     * @param linkRenderer the LinkRenderer to use
     * @return the HTML
     */
    public String markdownToHtml(char[] markdownSource, LinkRenderer linkRenderer) {
	    return markdownToHtml(markdownSource, linkRenderer, Collections.<String, VerbatimSerializer>emptyMap());
    }

	public String markdownToHtml(char[] markdownSource, LinkRenderer linkRenderer, Map<String, VerbatimSerializer> verbatimSerializerMap) {
        try {
            RootNode astRoot = parseMarkdown(markdownSource);
            return new ToHtmlSerializer(linkRenderer, verbatimSerializerMap).toHtml(astRoot);
        } catch(ParsingTimeoutException e) {
            return null;
        }
    }

    /**
     * Parses the given markdown source and returns the root node of the generated Abstract Syntax Tree.
     * If the input cannot be parsed within the configured parsing timeout the method throws a ParsingTimeoutException.
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
