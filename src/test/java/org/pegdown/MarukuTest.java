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

public class MarukuTest extends AbstractTest {

    private final PegDownProcessor processor = new PegDownProcessor(
            Extensions.ALL & ~Extensions.HARDWRAPS);

    @Override
    public PegDownProcessor getProcessor() {
        return processor;
    }

    @Test
    public void marukuTests() throws Exception {
        test("Maruku/abbreviations");
        test("Maruku/alt");
        test("Maruku/blank");
        test("Maruku/blanks_in_code");
        // test("Maruku/bug_def");
        // test("Maruku/bug_table");
        test("Maruku/code");
        test("Maruku/code2");
        test("Maruku/code3");
        test("Maruku/data_loss");
        test("Maruku/easy");
        test("Maruku/email");
        test("Maruku/entities");
        test("Maruku/escaping");
        // test("Maruku/extra_dl");
        // test("Maruku/extra_header_id");
        test("Maruku/extra_table1");
        // test("Maruku/footnotes");
        test("Maruku/headers");
        test("Maruku/hex_entities");
        // test("Maruku/hrule");
        // test("Maruku/html2");
        test("Maruku/html3");
        // test("Maruku/html4");
        // test("Maruku/html5");
        // test("Maruku/ie");
        test("Maruku/images");
        test("Maruku/images2");
        //test("Maruku/inline_html");
        //test("Maruku/inline_html2");
        test("Maruku/links");
        test("Maruku/list1");
        test("Maruku/list2");
        test("Maruku/list3");
        //test("Maruku/list4");
        test("Maruku/lists");
        //test("Maruku/lists11");
        test("Maruku/lists6");
        //test("Maruku/lists7");
        test("Maruku/lists7b");
        test("Maruku/lists8");
        //test("Maruku/lists9");
        //test("Maruku/lists_after_paragraph");
        test("Maruku/lists_ol");
        //test("Maruku/loss");
        //test("Maruku/misc_sw");
        test("Maruku/olist");
        test("Maruku/one");
        test("Maruku/paragraph");
        test("Maruku/paragraphs");
        test("Maruku/smartypants");
    }

}