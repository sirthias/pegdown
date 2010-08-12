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

public class CustomPegDownTest extends AbstractTest {

    private final PegDownProcessor processor = new PegDownProcessor(Extensions.ALL);

    @Override
    public PegDownProcessor getProcessor() {
        return processor;
    }

    @Test
    public void customPegDownTests() throws Exception {
        test("pegdown/Abbreviations");
        test("pegdown/Autolinks");
        test("pegdown/Bug_in_0.8.5.1");
        test("pegdown/Linebreaks");
        test("pegdown/Parens_in_URL");
        test("pegdown/Quoted Blockquote");

        test("pegdown/Smartypants");
        test("pegdown/Tables");
    }

}