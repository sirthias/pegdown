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

package org.pegdown;

import org.parboiled.common.StringUtils;
import org.pegdown.ast.AbbreviationNode;
import org.pegdown.ast.Node;
import org.pegdown.ast.ReferenceNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Printer {

    public final Map<String, ReferenceNode> references = new HashMap<String, ReferenceNode>();
    public final Map<String, String> abbreviations = new HashMap<String, String>();
    private final StringBuilder sb = new StringBuilder();
    private int indent; // the current output indent

    public Printer(List<ReferenceNode> references, List<AbbreviationNode> abbreviations) {
        for (ReferenceNode node : references) {
            this.references.put(printToString(node), node);
        }
        for (AbbreviationNode node : abbreviations) {
            this.abbreviations.put(printToString(node), encode(printToString(node.getExpansion())));
        }
    }

    public Printer indent(int delta) {
        indent += delta;
        return this;
    }

    public Printer print(String string) {
        if (StringUtils.isNotEmpty(string)) sb.append(string);
        return this;
    }

    public Printer print(char c) {
        sb.append(c);
        return this;
    }

    public Printer printIndent() {
        for (int i = 0; i < indent; i++) {
            print(' ');
        }
        return this;
    }

    public Printer println() {
        if (sb.length() > 0) {
            print('\n');
        }
        return this;
    }

    public Printer printChildrenWithAbbreviations(Node node) {
        if (abbreviations.isEmpty()) return printChildren(node);

        String string = printToString(node);
        for (Map.Entry<String, String> entry : abbreviations.entrySet()) {
            String abbr = entry.getKey();
            int ix = string.indexOf(abbr);
            if (ix == -1) continue;
            StringBuilder sb = new StringBuilder();
            int start = 0;
            String text = entry.getValue();
            while (ix >= 0) {
                sb.append(string.substring(start, ix));
                sb.append("<abbr");
                if (StringUtils.isNotEmpty(text)) sb.append(" title=\"").append(text).append('"');
                sb.append('>');
                sb.append(abbr);
                sb.append("</abbr>");
                start = ix + abbr.length();
                ix = string.indexOf(abbr, start);
            }
            sb.append(string.substring(start));
            string = sb.toString();
        }
        return print(string);
    }

    public Printer printOnNL(String string) {
        println();
        printIndent();
        print(string);
        return this;
    }

    public Printer printEncoded(String string) {
        return print(encode(string));
    }

    public Printer printChildren(Node node) {
        List<Node> children = node.getChildren();
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            Node child = children.get(i);
            child.print(this);
        }
        return this;
    }

    public String printToString(Node node) {
        int len = sb.length();
        printChildren(node);
        if (sb.length() == len) return "";
        String text = sb.substring(len, sb.length());
        sb.setLength(len);
        return text.replace("\n ", " ").replace(" \n", " ").replace('\n', ' ');
    }

    public static String encode(String string) {
        if (string == null) return null;
        for (int i = 0; i < string.length(); i++) {
            if (encode(string.charAt(i)) != null) {
                StringBuilder sb = new StringBuilder();
                for (i = 0; i < string.length(); i++) {
                    char c = string.charAt(i);
                    String encoded = encode(c);
                    if (encoded != null) {
                        sb.append(encoded);
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }
        }
        return string;
    }

    public static String encode(char c) {
        switch (c) {
            case '&':
                return "&amp;";
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            case '"':
                return "&quot;";
            default:
                return null;
        }
    }

    public String getString() {
        return sb.toString();
    }
}
