package org.pegdown;

import org.testng.annotations.Test;

import static org.pegdown.TestUtils.assertEqualsMultiline;

public class PegDownProcessorTest {

    @Test
    public void testToHtml() throws Exception {
        PegDownProcessor processor = new PegDownProcessor();
        String markdown = FileUtils.readAllTextFromResource("Amps and angle encoding.text");
        assertEqualsMultiline(processor.toHtml(markdown), "");
    }

}
