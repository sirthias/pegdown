package org.pegdown;

import org.parboiled.support.ToStringFormatter;
import org.testng.annotations.Test;

import static org.parboiled.trees.GraphUtils.printTree;
import static org.pegdown.TestUtils.assertEqualsMultiline;

public class PegDownProcessorTest {

    private final PegDownProcessor processor = new PegDownProcessor();

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
        String expected = FileUtils.readAllTextFromResource(testName + ".html");
        assertEqualsMultiline(toHtml, expected);
    }

}
