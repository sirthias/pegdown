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
import org.parboiled.Rule;
import org.parboiled.common.FileUtils;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.pegdown.ast.Node;
import org.testng.annotations.Test;
import static org.pegdown.Extensions.*;

public class CustomPegDownTest extends AbstractTest {

    /*private final PegDownProcessor processor = new PegDownProcessor(
            Parboiled.createParser(Parser.class, Extensions.ALL,
                    new Parser.ParseRunnerProvider() {
                        public ParseRunner<Node> get(Rule rule) {
                            return new TracingParseRunner<Node>(rule);
                        }
                    }
            ), 4
    );*/
    
    private PegDownProcessor processor = new PegDownProcessor(ALL);

    @Override
    public PegDownProcessor getProcessor() {
        return processor;
    }

    @Test
    public void customPegDownTests() {
        test("pegdown/Abbreviations");
        test("pegdown/Autolinks");
        test("pegdown/Bug_in_0.8.5.1");
        test("pegdown/Bug_in_0.8.5.4");
        test("pegdown/Bug_in_1.0.0");
        test("pegdown/GFM_Fenced_Code_Blocks");
        test("pegdown/Linebreaks");
        test("pegdown/Parens_in_URL");
        test("pegdown/Quoted Blockquote");
        test("pegdown/Smartypants");
        test("pegdown/Tables");
    }
    
    @Test(dependsOnMethods = "customPegDownTests")
    public void testASTIndices() {
        testAST("pegdown/AstText");
    }
    
    @Test(dependsOnMethods = "testASTIndices")
    public void customPegDownTests2() {
        processor = new PegDownProcessor(NONE);
        test("pegdown/Special Chars");
    }

    @Test(dependsOnMethods = "customPegDownTests2")
    public void testHTMLSuppression() {
        test("pegdown/HTML suppression", "" +
                "<h1>HTML <b>SUPPRESSION</b></h1>\n" +
                "<p>This is a paragraph containing a <strong>strong</strong> inline\n" +
                "HTML element and:</p>\n" +
                "<div>\n" +
                "<p>an actual block of HTML!</p>\n" +
                "</div>\n" +
                "\n");

        processor = new PegDownProcessor(SUPPRESS_INLINE_HTML);
        test("pegdown/HTML suppression", "" +
                "<h1>HTML SUPPRESSION</h1>\n" +
                "<p>This is a paragraph containing a strong inline HTML element\n" +
                "and:</p>\n" +
                "<div>\n" +
                "<p>an actual block of HTML!</p>\n" +
                "</div>\n" +
                "\n");

        processor = new PegDownProcessor(SUPPRESS_HTML_BLOCKS);
        test("pegdown/HTML suppression", "" +
                "<h1>HTML <b>SUPPRESSION</b></h1>\n" +
                "<p>This is a paragraph containing a <strong>strong</strong> inline\n" +
                "HTML element and:</p>\n" +
                "\n");

        processor = new PegDownProcessor(SUPPRESS_ALL_HTML);
        test("pegdown/HTML suppression", "" +
                "<h1>HTML SUPPRESSION</h1>\n" +
                "<p>This is a paragraph containing a strong inline HTML element\n" +
                "and:</p>\n" +
                "\n");
    }
    
    @Test(dependsOnMethods = "customPegDownTests2")
    public void testNoFollowLinks() {
        processor = new PegDownProcessor((ALL + NO_FOLLOW_LINKS) & ~HARDWRAPS);
        test("pegdown/No Follow Links");
    }

}