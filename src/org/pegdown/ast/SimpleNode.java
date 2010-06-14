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

public class SimpleNode extends Node implements SimpleNodeTypes {

    private final int type;

    public SimpleNode(int type) {
        this.type = type;
    }

    @Override
    public void print(Printer p) {
        switch (type) {
            case APOSTROPHE:
                p.print("&rsquo;");
                return;
            case ELLIPSIS:
                p.print("&hellip;");
                return;
            case EMDASH:
                p.print("&mdash;");
                return;
            case ENDASH:
                p.print("&ndash;");
                return;
            case HRULE:
                p.printOnNL("<hr/>");
                return;
            case LINEBREAK:
                p.print("<br/>").println();
                return;
        }

        throw new IllegalStateException();
    }

}