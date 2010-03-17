package org.pegdown;

import org.parboiled.trees.MutableTreeNodeImpl;

public class AstNode extends MutableTreeNodeImpl<AstNode> {

    public int type;
    public String text;

    public AstNode setType(int type) {
        this.type = type;
        return this;
    }

    public AstNode setText(String text) {
        this.text = text;
        return this;
    }
}
