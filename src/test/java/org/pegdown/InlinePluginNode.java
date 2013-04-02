package org.pegdown;

import org.pegdown.ast.Node;
import org.pegdown.ast.TextNode;
import org.pegdown.ast.Visitor;

public class InlinePluginNode extends TextNode {
    public InlinePluginNode(String text) {
        super(text);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit((Node) this);
    }
}
