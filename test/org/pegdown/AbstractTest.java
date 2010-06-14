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

import org.parboiled.support.ParsingResult;
import org.parboiled.support.ToStringFormatter;
import org.pegdown.ast.Node;
import org.testng.annotations.BeforeClass;
import org.w3c.tidy.Tidy;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static org.parboiled.trees.GraphUtils.printTree;
import static org.pegdown.PegDownProcessor.prepare;
import static org.pegdown.TestUtils.assertEqualsMultiline;
import static org.testng.Assert.assertNotNull;

public abstract class AbstractTest {

    private final Tidy tidy = new Tidy();

    @BeforeClass
    public void setup() {
        tidy.setPrintBodyOnly(true);
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);
    }

    protected abstract PegDownProcessor getProcessor();

    protected void test(String testName) {
        String markdown = FileUtils.readAllTextFromResource(testName + ".text");
        String actualHtml = getProcessor().markdownToHtml(markdown);

        // debugging I: check the actual (untidied) HTML
        // assertEqualsMultiline(actualHtml, "");

        // debugging II: check the AST
        // assertEqualsMultiline(printTree(getAstRoot(markdown), new ToStringFormatter<Node>()), "");

        // debugging III: check the parse tree
        // assertEqualsMultiline(printNodeTree(result), "");

        // tidy up html for fair equality test
        String expectedUntidy = FileUtils.readAllTextFromResource(testName + ".html");
        assertNotNull(expectedUntidy);

        actualHtml = tidy(actualHtml);
        assertEqualsMultiline(actualHtml, tidy(expectedUntidy));
    }

    private Node getAstRoot(String markdown) {
        ParsingResult<Node> result = getProcessor().getParser().parseRawBlock(prepare(markdown));
        return result.parseTreeRoot.getValue();
    }

    private String tidy(String html) {
        Reader in = new StringReader(html);
        Writer out = new StringWriter();
        tidy.parse(in, out);
        return out.toString();
    }

}