package org.pegdown;

import org.parboiled.support.ToStringFormatter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.tidy.Tidy;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static org.parboiled.trees.GraphUtils.printTree;
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
    }

    private void test(String testName) {
        String markdown = FileUtils.readAllTextFromResource(testName + ".text");

        AstNode astRoot = processor.getParser().parseRawBlock(markdown).parseTreeRoot.getValue();
        String astPrintout = FileUtils.readAllTextFromResource(testName + ".ast.text");
        assertEqualsMultiline(printTree(astRoot, new ToStringFormatter<AstNode>()), astPrintout);

        String toHtml = processor.toHtml(markdown);
        assertEqualsMultiline(toHtml, FileUtils.readAllTextFromResource(testName + ".pretty.html"));

        // tidy up html for fair equality test
        toHtml = tidy(toHtml);
        String expected = tidy(FileUtils.readAllTextFromResource(testName + ".html"));
        assertEqualsMultiline(toHtml, expected);
    }

    private String tidy(String html) {
        Reader in = new StringReader(html);
        Writer out = new StringWriter();
        tidy.parse(in, out);
        return out.toString();
    }


}
