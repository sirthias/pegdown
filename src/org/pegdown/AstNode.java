package org.pegdown;

import org.parboiled.trees.MutableTreeNodeImpl;
import org.parboiled.trees.TreeUtils;

/**
 * Very simple Abstract Syntax Tree node implementation based on a {@link org.parboiled.trees.MutableTreeNodeImpl}.
 * Defines two fields and some convenience setters.
 */
public class AstNode extends MutableTreeNodeImpl<AstNode> implements AstNodeType {

    private int type;
    private String text;

    public AstNode() {
        this(DEFAULT);
    }

    public AstNode(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
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
