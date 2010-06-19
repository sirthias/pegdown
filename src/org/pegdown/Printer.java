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
import org.pegdown.ast.TextNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class encapsulating most output functionality (i.e. AST-to-String serialization).
 */
public class Printer {

    public final Map<String, ReferenceNode> references = new HashMap<String, ReferenceNode>();
    public final Map<String, String> abbreviations = new HashMap<String, String>();
    private final StringBuilder sb = new StringBuilder();
    private int indent; // the current output indent

    public Printer(List<ReferenceNode> references, List<AbbreviationNode> abbreviations) {
        for (ReferenceNode node : references) {
            this.references.put(printToString(node).toLowerCase(), node);
        }

        // only fill the abbreviation map after having build all keys, so we do not expand abbreviations too early
        Map<String, String> abbrevs = new HashMap<String, String>();
        for (AbbreviationNode node : abbreviations) {
            abbrevs.put(printToString(node), encode(printToString(node.getExpansion())));
        }
        this.abbreviations.putAll(abbrevs);
    }

    public Printer indent(int delta) {
        indent += delta;
        return this;
    }

    public Printer print(String string) {
        if (StringUtils.isNotEmpty(string)) {
            sb.append(string);
        }
        return this;
    }

    public Printer printWithAbbreviations(String string) {
        if (abbreviations.isEmpty()) return print(string);
        Map<Integer, Map.Entry<String, String>> expansions = null;

        for (Map.Entry<String, String> entry : abbreviations.entrySet()) {
            // first check, whether we have a legal match
            String abbr = entry.getKey();

            int ix = 0;
            while (true) {
                int sx = string.indexOf(abbr, ix);
                if (sx == -1) break;

                // only allow whole word matches
                ix = sx + abbr.length();

                if (sx > 0 && Character.isLetterOrDigit(string.charAt(sx - 1))) continue;
                if (ix < string.length() && Character.isLetterOrDigit(string.charAt(ix))) {
                    continue;
                }

                // ok, legal match so save an expansions "task" for all matches
                if (expansions == null) {
                    expansions = new TreeMap<Integer, Map.Entry<String, String>>();
                }
                expansions.put(sx, entry);
            }
        }

        if (expansions != null) {
            StringBuilder sb = new StringBuilder();
            int ix = 0;
            for (Map.Entry<Integer, Map.Entry<String, String>> entry : expansions.entrySet()) {
                int sx = entry.getKey();
                String abbr = entry.getValue().getKey();
                String expansion = entry.getValue().getValue();

                sb.append(string.substring(ix, sx));

                StringBuilder replaceSB = new StringBuilder("<abbr");
                if (StringUtils.isNotEmpty(expansion)) replaceSB.append(" title=\"").append(expansion).append('"');
                replaceSB.append('>').append(abbr).append("</abbr>");
                String replace = replaceSB.toString();

                sb.append(replace);
                ix = sx + abbr.length();
            }
            sb.append(string.substring(ix));
            string = sb.toString();
        }
        return print(string);
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
        StringBuilder sb = new StringBuilder();
        List<Node> children = node.getChildren();
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            Node child = children.get(i);
            if (child instanceof TextNode) {
                sb.append(child.getText());
            } else {
                if (sb.length() > 0) {
                    printWithAbbreviations(sb.toString());
                    sb.setLength(0);
                }
                child.print(this);
            }
        }
        if (sb.length() > 0) printWithAbbreviations(sb.toString());
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
