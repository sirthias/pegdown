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

import org.parboiled.common.FileUtils;
import org.parboiled.common.Preconditions;
import org.parboiled.support.ToStringFormatter;
import org.pegdown.ast.Node;
import org.pegdown.ast.RootNode;
import org.testng.annotations.BeforeClass;
import org.w3c.tidy.Tidy;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static org.parboiled.support.ParseTreeUtils.printNodeTree;
import static org.parboiled.trees.GraphUtils.printTree;
import static org.pegdown.TestUtils.assertEqualsMultiline;
import static org.testng.Assert.assertEquals;
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
        String expectedUntidy = FileUtils.readAllTextFromResource(testName + ".html");
        assertNotNull(expectedUntidy);

        test(testName, tidy(expectedUntidy));
    }

    protected void test(String testName, String expectedOutput) {
        char[] markdown = FileUtils.readAllCharsFromResource(testName + ".md");
        Preconditions.checkState(markdown != null, "Test not found");
        
        RootNode astRoot = getProcessor().parseMarkdown(markdown);
        String actualHtml = new ToHtmlSerializer().toHtml(astRoot);

        // debugging I: check the parse tree
        //assertEquals(printNodeTree(getProcessor().parser.parseToParsingResult(markdown)), "<parse tree>");

        // debugging II: check the AST
        //assertEquals(printTree(astRoot, new ToStringFormatter<Node>()), "");

        // debugging III: check the actual (untidied) HTML
        //assertEquals(actualHtml, "");

        // tidy up html for fair equality test
        actualHtml = tidy(actualHtml);
        assertEqualsMultiline(actualHtml, expectedOutput);
    }
    
    @SuppressWarnings( {"ConstantConditions"})
    protected void testAST(String testName) {        
        String markdown = FileUtils.readAllTextFromResource(testName + ".md").replace("\r\n", "\n");
        Preconditions.checkState(markdown != null, "Test not found");
        
        String expectedAst = FileUtils.readAllTextFromResource(testName + ".ast");
        assertNotNull(expectedAst);
        
        RootNode astRoot = getProcessor().parseMarkdown(markdown.toCharArray());
        
        // check parse tree
        //assertEquals(printNodeTree(getProcessor().parser.parseToParsingResult(markdown)), "<parse tree>");

        assertEqualsMultiline(printTree(astRoot, new ToStringFormatter<Node>()), expectedAst);
    }

    private String tidy(String html) {
        Reader in = new StringReader(html);
        Writer out = new StringWriter();
        tidy.parse(in, out);
        return out.toString();
    }

}