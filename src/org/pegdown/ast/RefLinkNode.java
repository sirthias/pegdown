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
import org.pegdown.Printer;

public class RefLinkNode extends Node {

    private String separatorSpace;
    private Node referenceKey;
    private boolean image;

    public RefLinkNode(Node firstChild) {
        super(firstChild);
    }

    public boolean setSeparatorSpace(String separatorSpace) {
        this.separatorSpace = separatorSpace;
        return true;
    }

    public boolean setReferenceKey(Node referenceKey) {
        this.referenceKey = referenceKey;
        return true;
    }

    public RefLinkNode asImage() {
        image = true;
        return this;
    }

    @Override
    public void print(Printer printer) {
        String key = printer.printToString(referenceKey != null ? referenceKey : this);
        ReferenceNode refNode = key != null ? printer.references.get(key) : null;
        if (refNode == null) {
            // "fake" reference link
            printer.print('[').printChildren(this).print(']');
            if (separatorSpace != null) {
                printer.print(separatorSpace).print('[');
                if (referenceKey != null) referenceKey.print(printer);
                printer.print(']');
            }
            return;
        }
        printer
                .print('<')
                .print(image ? "img src" : "a href")
                .print("=\"")
                .printEncoded(refNode.getUrl())
                .print('"');
        if (refNode.getTitle() != null) {
            printer
                    .print(' ')
                    .print(image ? "alt" : "title")
                    .print("=\"")
                    .printEncoded(refNode.getTitle())
                    .print('"');
        }
        printer.print('>');

        printer.printChildren(this);

        printer.print("</").print(image ? "img" : "a").print('>');
    }
}