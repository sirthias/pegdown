/*
 * Copyright (C) 2010-2011 Mathias Doenitz
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
import org.pegdown.ast.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.parboiled.common.Preconditions.checkArgNotNull;

public class ToHtmlSerializer implements Visitor, Printer.Encoder {
    private Printer printer = new Printer();
    private final Map<String, ReferenceNode> references = new HashMap<String, ReferenceNode>();
    private final Map<String, String> abbreviations = new HashMap<String, String>();

    private TableNode currentTableNode;
    private int currentTableColumn;
    private boolean inTableHeader;
    private Random random = new Random(0x2626); // for email obfuscation 

    public String toHtml(RootNode astRoot) {
        checkArgNotNull(astRoot, "astRoot");
        astRoot.accept(this);
        return printer.getString();
    }
    
    public void visit(RootNode node) {
        for (ReferenceNode refNode : node.getReferences()) {
            visitChildren(refNode);
            references.put(normalize(printer.getString()), refNode);
            printer.clear();
        }
        for (AbbreviationNode abbrNode : node.getAbbreviations()) {
            visitChildren(abbrNode);
            String abbr = printer.getString();
            printer.clear();
            abbrNode.getExpansion().accept(this);
            String expansion = printer.getString();
            abbreviations.put(abbr, expansion);
            printer.clear();
        }
        visitChildren(node);
    }

    public void visit(AbbreviationNode node) {

    }

    public void visit(AutoLinkNode node) {
        printer.print("<a href=\"")
                .printEncoded(node.getText(), this)
                .print("\">")
                .printEncoded(node.getText(), this)
                .print("</a>");
    }

    public void visit(BlockQuoteNode node) {
        printIndentedTag(node, "blockquote");
    }

    public void visit(BulletListNode node) {
        printIndentedTag(node, "ul");
    }

    public void visit(CodeNode node) {
        printTag(node, "code");
    }

    public void visit(DefinitionListNode node) {
        printIndentedTag(node, "dl");
    }

    public void visit(DefinitionNode node) {
        printTag(node, "dd");
    }

    public void visit(DefinitionTermNode node) {
        printTag(node, "dt");
    }

    public void visit(EmphNode node) {
        printTag(node, "em");
    }

    public void visit(ExpLinkNode node) {
        if (node.getImage()) {
            printer.print("<img src=\"").printEncoded(node.getUrl(), this).print("\"  alt=\"").startEncoding(this);
            visitChildren(node);
            printer.stopEncoding().print("\"/>");
        } else {
            printer.print("<a href=\"").printEncoded(node.getUrl(), this).print('"');
            if (node.getTitle() != null) {
                printer.print(" title=\"").printEncoded(node.getTitle(), this).print('"');
            }
            printer.print('>');
            visitChildren(node);
            printer.print("</a>");
        }
    }

    public void visit(HeaderNode node) {
        printTag(node, "h" + node.getLevel());
    }

    public void visit(HtmlBlockNode node) {
        String text = node.getText();
        if (!text.isEmpty()) printer.println();
        printer.print(text);
    }

    public void visit(InlineHtmlNode node) {
        printer.print(node.getText());
    }

    public void visit(ListItemNode node) {
        printer.println();
        printTag(node, "li");
    }

    public void visit(MailLinkNode node) {
        printer.print("<a href=\"mailto:").print(obfuscate(node.getText())).print("\">")
                .print(obfuscate(node.getText()))
                .print("</a>");
    }

    public void visit(OrderedListNode node) {
        printIndentedTag(node, "ol");
    }

    public void visit(ParaNode node) {
        printTag(node, "p");
    }

    public void visit(QuotedNode node) {
        switch (node.getType()) {
            case DoubleAngle:
                printer.print("&laquo;");
                visitChildren(node);
                printer.print("&raquo;");
                break;
            case Double:
                printer.print("&ldquo;");
                visitChildren(node);
                printer.print("&rdquo;");
                break;
            case Single:
                printer.print("&lsquo;");
                visitChildren(node);
                printer.print("&rsquo;");
                break;
        }
    }

    public void visit(ReferenceNode node) {
        // reference nodes are not printed
    }

    public void visit(final RefLinkNode node) {
        String key = printToString(new Runnable() {
            public void run() {
                SuperNode keyNode = node.getReferenceKey() != null ? node.getReferenceKey() : node;
                visitChildren(keyNode);
            }
        });

        ReferenceNode refNode = key != null ? references.get(normalize(key)) : null;
        if (refNode == null) {
            // "fake" reference link
            printer.print('[');
            visitChildren(node);
            printer.print(']');
            if (node.getSeparatorSpace() != null) {
                printer.print(node.getSeparatorSpace()).print('[');
                if (node.getReferenceKey() != null) node.getReferenceKey().accept(this);
                printer.print(']');
            }
            return;
        }

        if (node.getImage()) {
            printer.print("<img src=\"").printEncoded(refNode.getUrl(), this).print("\"  alt=\"").startEncoding(this);
            visitChildren(node);
            printer.stopEncoding().print("\"/>");
        } else {
            printer.print("<a href=\"").printEncoded(refNode.getUrl(), this).print('"');
            if (refNode.getTitle() != null) {
                printer.print(" title=\"").printEncoded(refNode.getTitle(), this).print('"');
            }
            printer.print('>');
            visitChildren(node);
            printer.print("</a>");
        }
    }

    public void visit(SimpleNode node) {
        switch (node.getType()) {
            case Apostrophe:
                printer.print("&rsquo;");
                break;
            case Ellipsis:
                printer.print("&hellip;");
                break;
            case Emdash:
                printer.print("&mdash;");
                break;
            case Endash:
                printer.print("&ndash;");
                break;
            case HRule:
                printer.println().print("<hr/>");
                break;
            case Linebreak:
                printer.print("<br/>");
                break;
            case Nbsp:
                printer.print("&nbsp;");
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public void visit(StrongNode node) {
        printTag(node, "strong");
    }

    public void visit(TableBodyNode node) {
        printIndentedTag(node, "tbody");
    }

    public void visit(TableCellNode node) {
        String tag = inTableHeader ? "th" : "td";
        TableColumnNode column = currentTableNode.getColumns().get(currentTableColumn);

        printer.println().print('<').print(tag);
        column.accept(this);
        if (node.getColSpan() > 1) printer.print(" colspan=\"").print(Integer.toString(node.getColSpan())).print('"');
        printer.print('>');
        visitChildren(node);
        printer.print('<').print('/').print(tag).print('>');

        currentTableColumn += node.getColSpan();
    }

    public void visit(TableColumnNode node) {
        switch (node.getAlignment()) {
            case None:
                break;
            case Left:
                printer.print(" align=\"left\"");
                break;
            case Right:
                printer.print(" align=\"right\"");
                break;
            case Center:
                printer.print(" align=\"center\"");
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public void visit(TableHeaderNode node) {
        inTableHeader = true;
        printIndentedTag(node, "thead");
        inTableHeader = false;
    }

    public void visit(TableNode node) {
        currentTableNode = node;
        printIndentedTag(node, "table");
        currentTableNode = null;
    }

    public void visit(TableRowNode node) {
        currentTableColumn = 0;
        printIndentedTag(node, "tr");
    }

    public void visit(VerbatimNode node) {
        printer.println().print("<pre><code>");
        String text = node.getText();
        while(text.charAt(0) == '\n') {
            printer.print("<br/>");
            text = text.substring(1);
        }
        printer.printEncoded(text, this);
        printer.print("</code></pre>");
    }

    public void visit(TextNode node) {
        if (abbreviations.isEmpty()) {
            printer.print(node.getText());
        } else {
            printWithAbbreviations(node.getText());
        }
    }

    public void visit(SpecialTextNode node) {
        printer.printEncoded(node.getText(), this);
    }

    public void visit(SuperNode node) {
        visitChildren(node);
    }

    public void visit(Node node) {
        // override this method for processing custom Node implementations
        throw new RuntimeException("Not implemented");
    }

    // Printer.Encoder

    public String encode(char c) {
        switch (c) {
            case '&':
                return "&amp;";
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            case '"':
                return "&quot;";
            case '\'':
                return "&#39;";
        }
        return null;
    }

    // helpers

    private void visitChildren(SuperNode node) {
        for (Node child : node.getChildren()) {
            child.accept(this);
        }
    }

    private void printTag(TextNode node, String tag) {
        printer
                .print('<').print(tag).print('>')
                .printEncoded(node.getText(), this)
                .print('<').print('/').print(tag).print('>');
    }

    private void printTag(SuperNode node, String tag) {
        printer.print('<').print(tag).print('>');
        visitChildren(node);
        printer.print('<').print('/').print(tag).print('>');
    }

    private void printIndentedTag(SuperNode node, String tag) {
        printer.println().print('<').print(tag).print('>').indent(+2);
        visitChildren(node);
        printer.indent(-2).println().print('<').print('/').print(tag).print('>');
    }

    private String printToString(Runnable runnable) {
        Printer priorPrinter = printer;
        printer = new Printer();
        runnable.run();
        String result = printer.getString();
        printer = priorPrinter;
        return result;
    }

    private String normalize(String string) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch(c) {
                case ' ':
                case '\n':
                case '\t':
                    continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
  
    private String obfuscate(String email) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < email.length(); i++) {
            char c = email.charAt(i);
            switch (random.nextInt(5)) {
                case 0:
                case 1:
                    sb.append("&#").append((int) c).append(';');
                    break;
                case 2:
                case 3:
                    sb.append("&#x").append(Integer.toHexString(c)).append(';');
                    break;
                case 4:
                    String encoded = encode(c);
                    if (encoded != null) sb.append(encoded); else sb.append(c);
            }
        }
        return sb.toString();
    }
    
    public void printWithAbbreviations(String string) {
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
            int ix = 0;
            for (Map.Entry<Integer, Map.Entry<String, String>> entry : expansions.entrySet()) {
                int sx = entry.getKey();
                String abbr = entry.getValue().getKey();
                String expansion = entry.getValue().getValue();

                printer.printEncoded(string.substring(ix, sx), this);
                printer.print("<abbr");
                if (StringUtils.isNotEmpty(expansion)) {
                    printer.print(" title=\"");
                    printer.printEncoded(expansion, this);
                    printer.print('"');
                }
                printer.print('>');
                printer.printEncoded(abbr, this);
                printer.print("</abbr>");
                ix = sx + abbr.length();
            }
            printer.print(string.substring(ix));
        } else {
            printer.print(string);
        }
    }
}
