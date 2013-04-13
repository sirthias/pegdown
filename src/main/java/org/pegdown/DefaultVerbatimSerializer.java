package org.pegdown;

import org.parboiled.common.StringUtils;
import org.pegdown.ast.VerbatimNode;

public class DefaultVerbatimSerializer implements VerbatimSerializer {
    public static final DefaultVerbatimSerializer INSTANCE = new DefaultVerbatimSerializer();

    @Override
    public void serialize(final VerbatimNode node, final Printer printer) {
        printer.println().print("<pre><code");
        if (!StringUtils.isEmpty(node.getType())) {
            printAttribute(printer, "class", node.getType());
        }
        printer.print(">");
        String text = node.getText();
        // print HTML breaks for all initial newlines
        while (text.charAt(0) == '\n') {
            printer.print("<br/>");
            text = text.substring(1);
        }
        printer.printEncoded(text);
        printer.print("</code></pre>");

    }

    private void printAttribute(final Printer printer, final String name, final String value) {
        printer.print(' ').print(name).print('=').print('"').print(value).print('"');
    }
}
