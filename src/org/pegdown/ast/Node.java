/*
 * Copyright (C) 2010 Mathias Doenitz
 *
 * Based on peg-markdown (C) 2008-2010 John MacFarlane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pegdown.ast;

import org.parboiled.common.StringUtils;
import org.parboiled.trees.MutableTreeNodeImpl;
import org.parboiled.trees.TreeUtils;
import org.pegdown.Printer;

import java.util.List;

/**
 * Base class of all AST nodes classes. Provides the basic infrastructure and can be used directly as a simple
 * intermediate AST node without any special functionality (i.e. as a basic parent node).
 */
public class Node extends MutableTreeNodeImpl<Node> {

    private String text;

    public Node() {
    }

    public Node(String text) {
        this.text = text;
    }

    public Node(Node firstChild) {
        addChild(firstChild);
    }

    public String getText() {
        return text;
    }

    public boolean addChild(Node child) {
        if (child != null) {
            TreeUtils.addChild(this, child);
        }
        return true;
    }

    public void print(Printer printer) {
        printer.printChildren(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        String text = getText();
        if (text != null) sb.append(" '").append(StringUtils.escape(text)).append('\'');
        return sb.toString();
    }

}
