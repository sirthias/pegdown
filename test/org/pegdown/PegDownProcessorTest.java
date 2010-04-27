package org.pegdown;

import org.parboiled.support.ParsingResult;
import org.parboiled.support.ToStringFormatter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.tidy.Tidy;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static org.parboiled.trees.GraphUtils.printTree;
import static org.pegdown.PegDownProcessor.prepare;
import static org.pegdown.TestUtils.assertEqualsMultiline;

public class PegDownProcessorTest {

    private final PegDownProcessor processor = new PegDownProcessor();
    private final Tidy tidy = new Tidy();

    @BeforeClass
    public void setup() {
        tidy.setPrintBodyOnly(true);
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);
    }

    @Test
    public void test() throws Exception {
        test("Amps and angle encoding");
        test("Auto links");
        test("Backslash escapes");
        test("Blockquotes with code blocks");
        test("Code Blocks");
        test("Code Spans");
        test("Hard-wrapped paragraphs with list-like lines");
        test("Horizontal rules");
        test("Inline HTML (Advanced)");
        test("Inline HTML (Simple)");
        test("Inline HTML comments");
        test("Links, inline style");
        test("Links, reference style");
        test("Links, shortcut references");
        test("Literal quotes in titles");
        test("Nested blockquotes");
        test("Ordered and unordered lists");
        test("Strong and em together");
        test("Tabs");
        test("Tidyness");

        test("Quoted Blockquote");
        
        test("Markdown Documentation - Basics");
        test("Markdown Documentation - Syntax");
    }

    private void test(String testName) {
        String markdown = FileUtils.readAllTextFromResource(testName + ".text");

        ParsingResult<AstNode> result = processor.getParser().parseRawBlock(prepare(markdown));
        // assertEqualsMultiline(printNodeTree(result), "");  // for advanced debugging: check the parse tree
        AstNode astRoot = result.parseTreeRoot.getValue();
        String expectedAst = FileUtils.readAllTextFromResource(testName + ".ast.text");
        String actualAst = printTree(astRoot, new ToStringFormatter<AstNode>());
        assertEqualsMultiline(actualAst, expectedAst);

        String expectedHtml = FileUtils.readAllTextFromResource(testName + ".compact.html");
        String actualHtml = processor.markDownToHtml(markdown);
        assertEqualsMultiline(actualHtml, expectedHtml);

        // tidy up html for fair equality test
        actualHtml = tidy(actualHtml);
        expectedHtml = tidy(FileUtils.readAllTextFromResource(testName + ".html"));
        assertEqualsMultiline(actualHtml, expectedHtml);
    }

    private String tidy(String html) {
        Reader in = new StringReader(html);
        Writer out = new StringWriter();
        tidy.parse(in, out);
        return out.toString();
    }


}
