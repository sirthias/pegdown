package org.pegdown;

import org.parboiled.trees.MutableTreeNodeImpl;
import org.parboiled.trees.TreeUtils;

public class AstNode extends MutableTreeNodeImpl<AstNode> implements AstNodeType {

    public int type;
    public String text;

    public AstNode() {
        this(DEFAULT);
    }

    public AstNode(int type) {
        this.type = type;
    }

    public AstNode withType(int type) {
        this.type = type;
        return this;
    }
    
    public boolean setType(int type) {
        this.type = type;
        return true;
    }

    public AstNode withText(String text) {
        this.text = text;
        return this;
    }
    
    public boolean setText(String text) {
        this.text = text;
        return true;
    }
    
    public AstNode withTextAdded(String text) {
        addText(text);
        return this;
    }

    public boolean addText(String text) {
        this.text = text.concat(text);
        return true;
    }
    
    public AstNode withChild(AstNode child) {
        addChild(child);
        return this;
    }

    public boolean addChild(AstNode child) {
        if (child != null) {
            TreeUtils.addChild(this, child);
        }
        return true;
    }

    @Override
    public String toString() {
        return text == null ? TYPE_NAMES[type] : TYPE_NAMES[type] + ": \"" + text + '"';
    }
}
