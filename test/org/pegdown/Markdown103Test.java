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

import org.testng.annotations.Test;

public class Markdown103Test extends AbstractTest {

    private final PegDownProcessor processor = new PegDownProcessor();

    @Override
    protected PegDownProcessor getProcessor() {
        return processor;
    }

    @Test
    public void markdownTestSuite103() throws Exception {
        test("MarkdownTest103/Amps and angle encoding");
        test("MarkdownTest103/Auto links");
        test("MarkdownTest103/Backslash escapes");
        test("MarkdownTest103/Blockquotes with code blocks");
        test("MarkdownTest103/Code Blocks");
        test("MarkdownTest103/Code Spans");
        test("MarkdownTest103/Hard-wrapped paragraphs with list-like lines");
        test("MarkdownTest103/Horizontal rules");
        test("MarkdownTest103/Inline HTML (Advanced)");
        test("MarkdownTest103/Inline HTML (Simple)");
        test("MarkdownTest103/Inline HTML comments");
        test("MarkdownTest103/Links, inline style");
        test("MarkdownTest103/Links, reference style");
        test("MarkdownTest103/Links, shortcut references");
        test("MarkdownTest103/Literal quotes in titles");
        test("MarkdownTest103/Nested blockquotes");
        test("MarkdownTest103/Ordered and unordered lists");
        test("MarkdownTest103/Strong and em together");
        test("MarkdownTest103/Tabs");
        test("MarkdownTest103/Tidyness");

        test("MarkdownTest103/Markdown Documentation - Basics");
        test("MarkdownTest103/Markdown Documentation - Syntax");
    }
    
}
