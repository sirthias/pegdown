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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.parboiled.common.StringUtils;
import org.pegdown.ast.AbbreviationNode;
import org.pegdown.ast.AutoLinkNode;
import org.pegdown.ast.BlockQuoteNode;
import org.pegdown.ast.BulletListNode;
import org.pegdown.ast.CodeNode;
import org.pegdown.ast.EmphNode;
import org.pegdown.ast.ExpLinkNode;
import org.pegdown.ast.HeaderNode;
import org.pegdown.ast.HtmlBlockNode;
import org.pegdown.ast.LooseListItemNode;
import org.pegdown.ast.MailLinkNode;
import org.pegdown.ast.Node;
import org.pegdown.ast.OrderedListNode;
import org.pegdown.ast.ParaNode;
import org.pegdown.ast.QuotedNode;
import org.pegdown.ast.RefLinkNode;
import org.pegdown.ast.ReferenceNode;
import org.pegdown.ast.SimpleNode;
import org.pegdown.ast.StrongNode;
import org.pegdown.ast.TableCellNode;
import org.pegdown.ast.TableColumnNode;
import org.pegdown.ast.TableNode;
import org.pegdown.ast.TableRowNode;
import org.pegdown.ast.TextNode;
import org.pegdown.ast.TightListItemNode;
import org.pegdown.ast.VerbatimNode;

/**
 * Helper class encapsulating most output functionality (i.e. AST-to-String serialization).
 */
@SuppressWarnings("serial")
public class Printer implements Visitor<Node> {
    public final Map<String, ReferenceNode> references = new HashMap<String, ReferenceNode>();
    public final Map<String, String> abbreviations = new HashMap<String, String>();
    protected final StringBuilder sb = new StringBuilder();
    protected int indent; // the current output indent
	
	public final Map<Class<? extends Node>, Visitor<? extends Node>> visitors = 
	new HashMap<Class<? extends Node>, Visitor<? extends Node>>() {
		{
			put(AutoLinkNode.class, new Visitor<AutoLinkNode>(){
				public void visit(AutoLinkNode node) {
			        print("<a href=\"")
	                .printEncoded(node.getText())
	                .print("\">")
	                .printEncoded(node.getText())
	                .print("</a>");
				}
			});
			
			put(BlockQuoteNode.class, new Visitor<BlockQuoteNode>(){
				public void visit(BlockQuoteNode node) {
			        printOnNL("<blockquote>")
	                .indent(+2).printChildren(node).indent(-2)
	                .printOnNL("</blockquote>");
				}
			});
			put(BulletListNode.class, new Visitor<BulletListNode>(){
				public void visit(BulletListNode node) {
					printOnNL("<ul>").indent(+2).printChildren(node).indent(-2).printOnNL("</ul>");
				}
			});
			put(CodeNode.class, new Visitor<CodeNode>(){
				public void visit(CodeNode node) {
					print("<code>").printEncoded(node.getText()).print("</code>");
				}
			});
			put(EmphNode.class, new Visitor<EmphNode>(){
				public void visit(EmphNode node) {
					print("<em>").printChildren(node).print("</em>");
				}
			});
			put(ExpLinkNode.class, new Visitor<ExpLinkNode>(){
				public void visit(ExpLinkNode node) {
			        if (node.getImage()) {
			            print("<img src=\"")
			                    .printEncoded(node.getUrl())
			                    .print("\"  alt=\"")
			                    .printEncoded(printToString(node))
			                    .print("\"/>");
			        } else {
			            print("<a href=\"")
			                    .printEncoded(node.getUrl())
			                    .print('"');
			            if (node.getTitle() != null) {
			                print(" title=\"")
			                        .printEncoded(node.getTitle())
			                        .print('"');
			            }
			            print('>')
			                    .printChildren(node)
			                    .print("</a>");
			        }
				}
			});
			put(HeaderNode.class, new Visitor<HeaderNode>(){
				public void visit(HeaderNode node) {
			        char c = (char) ((int)'0' + node.getLevel());
			        print("<h").print(c).print('>').printChildren(node).print("</h").print(c).print('>');
				}
			});
			put(HtmlBlockNode.class, new Visitor<HtmlBlockNode>(){
				public void visit(HtmlBlockNode node) {
					printOnNL(node.getText());
				}
			});
			put(LooseListItemNode.class, new Visitor<LooseListItemNode>(){
				public void visit(LooseListItemNode node) {
					printOnNL("<li>").indent(+2).printChildren(node).indent(-2).printOnNL("</li>");
				}
			});
			put(MailLinkNode.class, new Visitor<MailLinkNode>(){
				public void visit(MailLinkNode node) {
					print("<a href=\"mailto:").printEncoded(node.getText()).print("\">").printEncoded(node.getText()).print("</a>");
				}
			});
			put(Node.class, new Visitor<Node>(){
				public void visit(Node node) {
					printChildren(node);
				}
			});
			put(OrderedListNode.class, new Visitor<OrderedListNode>(){
				public void visit(OrderedListNode node) {
					printOnNL("<ol>").indent(+2).printChildren(node).indent(-2).printOnNL("</ol>");
				}
			});
			put(ParaNode.class, new Visitor<ParaNode>(){
				public void visit(ParaNode node) {
					printOnNL("<p>").printChildren(node).print("</p>");
				}
			});
			put(QuotedNode.class, new Visitor<QuotedNode>(){
				public void visit(QuotedNode node) {
					print(node.getOpen()).printChildren(node).print(node.getClose());
				}
			});
			put(RefLinkNode.class, new Visitor<RefLinkNode>(){
				public void visit(RefLinkNode node) {
			        String key = printToString(node.getReferenceKey() != null ? node.getReferenceKey() : node);
			        ReferenceNode refNode = key != null ? references.get(key.toLowerCase()) : null;
			        if (refNode == null) {
			            // "fake" reference link
			            print('[').printChildren(node).print(']');
			            if (node.getSeparatorSpace() != null) {
			                print(node.getSeparatorSpace()).print('[');
			                if (node.getReferenceKey() != null) node.getReferenceKey().accept(Printer.this);
			                print(']');
			            }
			            return;
			        }

			        if (node.getImage()) {
			            print("<img src=\"")
			                    .printEncoded(refNode.getUrl())
			                    .print("\"  alt=\"")
			                    .printEncoded(printToString(node))
			                    .print("\"/>");
			        } else {
			            print("<a href=\"")
			                    .printEncoded(refNode.getUrl())
			                    .print('"');
			            if (refNode.getTitle() != null) {
			                print(" title=\"")
			                        .printEncoded(refNode.getTitle())
			                        .print('"');
			            }
			            print('>')
			                    .printChildren(node)
			                    .print("</a>");
			        }
				}
			});
			put(SimpleNode.class, new Visitor<SimpleNode>(){
				public void visit(SimpleNode node) {
			        switch (node.getType()) {
		            case SimpleNode.APOSTROPHE:
		            case SimpleNode.ELLIPSIS:
		            case SimpleNode.EMDASH:
		            case SimpleNode.ENDASH:
		            case SimpleNode.NBSP:
		                print(node.getText());
		                return;
		            case SimpleNode.HRULE:
		                printOnNL("<hr/>");
		                return;
		            case SimpleNode.LINEBREAK:
		                print("<br/>").println();
		                return;
		        }

		        throw new IllegalStateException();
				}
			});
			put(StrongNode.class, new Visitor<StrongNode>(){
				public void visit(StrongNode node) {
					print("<strong>").printChildren(node).print("</strong>");
				}
			});
			put(TableNode.class, new Visitor<TableNode>(){
				public void visit(TableNode node) {
			        printOnNL("<table>").indent(+2);

			        boolean inHeader = false;
			        List<Node> children = node.getChildren();
			        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
			            TableRowNode rowNode = (TableRowNode) children.get(i);

			            if (i == 0) {
			                inHeader = rowNode.isHeader();
			                printOnNL(inHeader ? "<thead>" : "<tbody>").indent(+2);
			            } else {
			                if (inHeader && !rowNode.isHeader()) {
			                    indent(-2).printOnNL("</thead>").printOnNL("<tbody>").indent(+2);
			                    inHeader = false;
			                }
			            }

			            print(rowNode, node.getColumns());
			        }
			        indent(-2).printOnNL(inHeader ? "</thead>" : "</tbody>");
			        indent(-2).printOnNL("</table>");
				}
			});
			put(TextNode.class, new Visitor<TextNode>(){
				public void visit(TextNode node) {
					print(node.getText());
				}
			});
			put(TightListItemNode.class, new Visitor<TightListItemNode>(){
				public void visit(TightListItemNode node) {
					printOnNL("<li>").printChildren(node).print("</li>");
				}
			});
			put(VerbatimNode.class, new Visitor<VerbatimNode>(){
				public void visit(VerbatimNode node) {
					printOnNL("<pre><code>").printEncoded(node.getText()).print("</code></pre>");
				}
			});
		}
	};

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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void visit(Node node) {
		Visitor visitor = visitors.get(node.getClass());
		if(visitor != null) {
			visitor.visit(node);
		}
	}
    
	/*
	 * Print functions for table structure
	 */
    void print(TableRowNode row, List<TableColumnNode> columns) {
        printOnNL("<tr>").indent(+2);
        List<Node> children = row.getChildren();

        int col = 0;
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            TableCellNode cell = (TableCellNode) children.get(i);
            print(cell, col < columns.size() ? columns.get(col) : null, row.isHeader());
            col += cell.getColSpan();
        }

        indent(-2).printOnNL("</tr>");
    }


    public void print(TableCellNode cell, TableColumnNode column, boolean header) {
        printOnNL(header ? "<th" : "<td");
        if (column != null) printAlignment(column);
        int colSpan = cell.getColSpan();
        if (colSpan > 1) print(" colspan=\"").print(Integer.toString(colSpan)).print('"');
        print('>');

        indent(+2).printChildren(cell).indent(-2);

        print(header ? "</th>" : "</td>");
    }
    
    
    public void printAlignment(TableColumnNode column) {
        switch (column.getAlignment()) {
            case 0x00:
                return;
            case 0x01:
                print(" align=\"left\"");
                return;
            case 0x02:
                print(" align=\"right\"");
                return;
            case 0x03:
                print(" align=\"center\"");
                return;
        }
        throw new IllegalStateException();
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
                child.accept(this);
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
