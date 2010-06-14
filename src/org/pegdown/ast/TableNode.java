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

import java.util.ArrayList;
import java.util.List;

public class TableNode extends Node {

    private List<TableColumnNode> columns;

    public boolean addColumn(TableColumnNode columnNode) {
        if (columns == null) {
            columns = new ArrayList<TableColumnNode>();
        }
        return columns.add(columnNode);
    }

    public boolean hasTwoOrMoreDividers() {
        return columns != null && columns.size() > 1;
    }

    @Override
    public void print(Printer printer) {
        printer.printOnNL("<table>").indent(+2);

        boolean inHeader = false;
        List<Node> children = getChildren();
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            TableRowNode rowNode = (TableRowNode) children.get(i);

            if (i == 0) {
                inHeader = rowNode.isHeader();
                printer.printOnNL(inHeader ? "<thead>" : "<tbody>").indent(+2);
            } else {
                if (inHeader && !rowNode.isHeader()) {
                    printer.indent(-2).printOnNL("</thead>").printOnNL("<tbody>").indent(+2);
                    inHeader = false;
                }
            }

            rowNode.print(printer, columns);
        }
        printer.indent(-2).printOnNL(inHeader ? "</thead>" : "</tbody>");
        printer.indent(-2).printOnNL("</table>");
    }

}