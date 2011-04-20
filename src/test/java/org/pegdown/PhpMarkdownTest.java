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

import org.testng.annotations.Test;

public class PhpMarkdownTest extends AbstractTest {

    private final PegDownProcessor processor = new PegDownProcessor(Extensions.NONE);

    @Override
    public PegDownProcessor getProcessor() {
        return processor;
    }

    @Test
    public void phpMarkdownTests() throws Exception {
        //test("PhpMarkdown/Backslash_escapes");
        test("PhpMarkdown/Code_block_in_a_list_item");
        test("PhpMarkdown/Code_Spans");
        //test("PhpMarkdown/Email_auto_links");
        //test("PhpMarkdown/Emphasis");
        test("PhpMarkdown/Headers");
        test("PhpMarkdown/Horizontal_Rules");
        test("PhpMarkdown/Inline_HTML_(Simple)");
        test("PhpMarkdown/Inline_HTML_(Span)");
        test("PhpMarkdown/Inline_HTML_comments");
        //test("PhpMarkdown/Ins_and_del");
        //test("PhpMarkdown/Links_inline_style");
        test("PhpMarkdown/MD5_Hashes");
        //test("PhpMarkdown/Nesting");
        //test("PhpMarkdown/Parens_in_URL");
        //test("PhpMarkdown/PHP-Specific_Bugs");
        test("PhpMarkdown/Tight_blocks");
    }

}