package org.pegdown;

import org.pegdown.ast.Node;

public interface Visitor<T extends Node> {

    public void visit(T node);
    
}