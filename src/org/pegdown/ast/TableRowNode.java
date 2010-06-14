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

import org.pegdown.Printer;

import java.util.List;

public class TableRowNode extends Node {

    private boolean header;

    public boolean isHeader() {
        return header;
    }
    
    public TableRowNode asHeader() {
        header = true;
        return this;
    }

    public void print(Printer printer, List<TableColumnNode> columns) {
        printer.printOnNL("<tr>").indent(+2);
        List<Node> children = getChildren();

        int col = 0;
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            TableCellNode cell = (TableCellNode) children.get(i);
            cell.print(printer, col < columns.size() ? columns.get(col) : null, header);
            col += cell.getColSpan();
        }

        printer.indent(-2).printOnNL("</tr>");
    }

}