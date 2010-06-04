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

import org.parboiled.Parboiled;
import org.parboiled.common.StringUtils;
import org.parboiled.google.base.Preconditions;
import org.parboiled.support.ParsingResult;

import java.util.HashMap;
import java.util.Map;

/**
 * A clean and lightweight Markdown-to-HTML filter based on a PEG parser implemented with parboiled.
 *
 * @see <a href="http://daringfireball.net/projects/markdown/">Markdown</a>
 * @see <a href="http://www.parboiled.org/">parboiled.org</a>
 */
public class PegDownProcessor implements AstNodeType {

    /**
     * Defines the number of spaces in a tab, can be changed externally if required.
     */
    public static int TABSTOP = 4;

    private final MarkDownParser parser;
    private final Map<String, String> refLinkUrls = new HashMap<String, String>();
    private final Map<String, String> refLinkTitles = new HashMap<String, String>();
    private final Map<String, String> abbreviations = new HashMap<String, String>();
    private StringBuilder sb;
    private int indent; // the current output indent

    /**
     * Creates a new processor instance without any enabled extensions.
     */
    public PegDownProcessor() {
        this(Extensions.NONE);
    }

    /**
     * Creates a new processor instance with the given {@link org.pegdown.Extensions}.
     *
     * @param options the flags of the extensions to enable as a bitmask
     */
    @SuppressWarnings({"unchecked"})
    public PegDownProcessor(int options) {
        parser = options == Extensions.NONE ?
                Parboiled.createParser(MarkDownParser.class) :
                Parboiled.createParser(ExtendedMarkDownParser.class, options);
    }

    /**
     * Returns the underlying parboiled parser object
     *
     * @return the parser
     */
    MarkDownParser getParser() {
        return parser;
    }

    /**
     * Converts the given markdown source to HTML.
     *
     * @param markdownSource the markdown source to convert
     * @return the HTML
     */
    public String markdownToHtml(String markdownSource) {
        parser.references.clear();
        parser.abbreviations.clear();
        refLinkUrls.clear();
        refLinkTitles.clear();
        indent = 0;

        ParsingResult<AstNode> result = parser.parseRawBlock(prepare(markdownSource));

        sb = new StringBuilder();
        buildRefLinkUrls();
        buildAbbreviations();

        print(result.parseTreeRoot.getValue()).println();

        return sb.toString();
    }

    // performs tabstop expansion and adds two trailing newlines

    static String prepare(String markDownSource) {
        StringBuilder sb = new StringBuilder(markDownSource.length() + 2);
        int charsToTab = TABSTOP;
        for (int i = 0; i < markDownSource.length(); i++) {
            char c = markDownSource.charAt(i);
            switch (c) {
                case '\t':
                    while (charsToTab > 0) {
                        sb.append(' ');
                        charsToTab--;
                    }
                    break;
                case '\n':
                    sb.append('\n');
                    charsToTab = TABSTOP;
                    break;
                default:
                    sb.append(c);
                    charsToTab--;
            }
            if (charsToTab == 0) charsToTab = TABSTOP;
        }
        sb.append('\n');
        sb.append('\n');
        return sb.toString();
    }

    @SuppressWarnings({"ConstantConditions"})
    private void buildRefLinkUrls() {
        refLinkUrls.clear();
        if (parser.references.isEmpty()) return;
        for (AstNode refNode : parser.references) {
            String key = printToString(child(refNode, LINK_LABEL)).toLowerCase();
            AstNode urlNode = child(refNode, LINK_URL);
            Preconditions.checkState(urlNode != null);
            String value = encode(urlNode.getText());
            refLinkUrls.put(key, value);

            AstNode titleNode = child(refNode, LINK_TITLE);
            if (titleNode != null) {
                String title = encode(titleNode.getText());
                refLinkTitles.put(key, title);
            }
        }
    }

    @SuppressWarnings({"ConstantConditions"})
    private void buildAbbreviations() {
        abbreviations.clear();
        if (parser.abbreviations.isEmpty()) return;
        for (AstNode refNode : parser.abbreviations) {
            String key = printToString(child(refNode, LINK_LABEL));
            AstNode titleNode = child(refNode, LINK_TITLE);
            Preconditions.checkState(titleNode != null);
            String value = encode(printToString(titleNode));
            abbreviations.put(key, value);
        }
    }

    private PegDownProcessor print(AstNode node) {
        if (node == null) return this;

        switch (node.getType()) {
            case DEFAULT:
                return printChildren(node);

            // basic elements
            case APOSTROPHE:
                return print("&rsquo;");
            case ELLIPSIS:
                return print("&hellip;");
            case EMDASH:
                return print("&mdash;");
            case ENDASH:
                return print("&ndash;");
            case HTML:
                return print(node.getText());
            case LINEBREAK:
                return printOnNL("<br/>").println();
            case SPACE:
                return print(node.getText());
            case SPECIAL:
                return printEncoded(node.getText().charAt(0));
            case TEXT:
                return print(node.getText());

            // inline groups
            case CODE:
                return print("<code>").printEncoded(node.getText()).print("</code>");
            case EMPH:
                return print("<em>").printChildren(node).print("</em>");
            case H1:
                return print("<h1>").printChildren(node).print("</h1>");
            case H2:
                return print("<h2>").printChildren(node).print("</h2>");
            case H3:
                return print("<h3>").printChildren(node).print("</h3>");
            case H4:
                return print("<h4>").printChildren(node).print("</h4>");
            case H5:
                return print("<h5>").printChildren(node).print("</h5>");
            case H6:
                return print("<h6>").printChildren(node).print("</h6>");
            case STRONG:
                return print("<strong>").printChildren(node).print("</strong>");
            case SINGLE_QUOTED:
                return print("&lsquo;").printChildren(node).print("&rsquo;");
            case DOUBLE_QUOTED:
                return print("&ldquo;").printChildren(node).print("&rdquo;");

            // blocks
            case BLOCKQUOTE:
                return printOnNL("<blockquote>").indent(+2).printChildren(node).indent(-2).printOnNL("</blockquote>");
            case HRULE:
                return printOnNL("<hr/>");
            case HTMLBLOCK:
                return printOnNL(node.getText());
            case PARA:
                return printOnNL("<p>").printChildrenWithAbbreviations(node).print("</p>");
            case VERBATIM:
                return printOnNL("<pre><code>").printEncoded(node.getText()).print("</code></pre>");

            // lists
            case BULLET_LIST:
                return printOnNL("<ul>").indent(+2).printChildren(node).indent(-2).printOnNL("</ul>");
            case ORDERED_LIST:
                return printOnNL("<ol>").indent(+2).printChildren(node).indent(-2).printOnNL("</ol>");
            case TIGHT_LIST_ITEM:
                return printOnNL("<li>").printChildren(node).print("</li>");
            case LOOSE_LIST_ITEM:
                return printOnNL("<li>").indent(+2).printChildren(node).indent(-2).printOnNL("</li>");

            // links
            case AUTO_LINK:
                return print("<a href=\"").printEncoded(node.getText()).print("\">").printEncoded(node.getText())
                        .print("</a>");
            case EXP_LINK:
                return print("<a href=\"").printChild(node, LINK_URL).print('"')
                        .printChild(node, LINK_TITLE).print('>')
                        .printChild(node, LINK_LABEL).print("</a>");
            case EXP_IMG_LINK:
                return print("<img src=\"").printChild(node, LINK_URL).print('"')
                        .print(" alt=\"").printChild(node, LINK_LABEL).print("\"")
                        .printChild(node, LINK_TITLE).print("/>");
            case MAIL_LINK:
                return print("<a href=\"mailto:").printEncoded(node.getText()).print("\">")
                        .printEncoded(node.getText()).print("</a>");
            case REF_LINK:
                return printReferenceLink(node);
            case REF_IMG_LINK:
                return printReferenceImageLink(node);
            case LINK_LABEL:
                return printChildren(node);
            case LINK_REF:
                return print('[').printChildren(node).print(']'); // only reached for fake reference links 
            case LINK_TITLE:
                return print(" title=\"").printEncoded(node.getText()).print('"');
            case LINK_URL:
                return printEncoded(node.getText());
            case REFERENCE:
            case ABBREVIATION:
                return this; // reference and abbreviation definitions are not printed
        }
        throw new IllegalStateException();
    }

    private PegDownProcessor indent(int delta) {
        indent += delta;
        return this;
    }

    private PegDownProcessor print(String string) {
        if (StringUtils.isNotEmpty(string)) sb.append(string);
        return this;
    }

    private PegDownProcessor print(char c) {
        sb.append(c);
        return this;
    }

    private PegDownProcessor printIndent() {
        for (int i = 0; i < indent; i++) {
            print(' ');
        }
        return this;
    }

    private PegDownProcessor println() {
        if (sb.length() > 0) {
            print('\n');
        }
        return this;
    }

    private PegDownProcessor printChildrenWithAbbreviations(AstNode node) {
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

    private PegDownProcessor printOnNL(String string) {
        println();
        printIndent();
        print(string);
        return this;
    }

    private PegDownProcessor printEncoded(String string) {
        return print(encode(string));
    }

    private PegDownProcessor printEncoded(char c) {
        String encoded = encode(c);
        return encoded != null ? print(encoded) : print(c);
    }

    private PegDownProcessor printReferenceLink(AstNode node) {
        return printReferenceLink(node, "<a href=\"", ">", "</a>");
    }

    private PegDownProcessor printReferenceImageLink(AstNode node) {
        return printReferenceLink(node, "<img src=\"", "\" alt=\"", "\"/>");
    }

    private PegDownProcessor printReferenceLink(AstNode node, String open, String middle, String end) {
        String linkRef = getLinkRef(node);
        String linkUrl = refLinkUrls.get(linkRef);
        if (linkUrl == null) {
            // "fake" reference link
            return print('[').printChild(node, LINK_LABEL).print(']').printChild(node, SPACE)
                    .printChild(node, LINK_REF);
        }
        print(open).print(linkUrl).print('"');
        String linkTitle = refLinkTitles.get(linkRef);
        if (StringUtils.isNotEmpty(linkTitle)) {
            print(" title=\"").print(linkTitle).print('"');
        }
        return print(middle).printChild(node, LINK_LABEL).print(end);
    }

    private String getLinkRef(AstNode node) {
        AstNode linkRefNode = child(node, LINK_REF);
        String linkRef = linkRefNode != null ? printToString(linkRefNode).toLowerCase() : null;
        return StringUtils.isNotEmpty(linkRef) ? linkRef : printToString(child(node, LINK_LABEL)).toLowerCase();
    }

    private PegDownProcessor printChildren(AstNode node) {
        for (AstNode child : node.getChildren()) {
            print(child);
        }
        return this;
    }

    private PegDownProcessor printChild(AstNode parent, int type) {
        return print(child(parent, type));
    }

    private String printToString(AstNode node) {
        int len = sb.length();
        printChildren(node);
        if (sb.length() == len) return "";
        String text = sb.substring(len, sb.length());
        sb.setLength(len);
        return text.replace("\n ", " ").replace(" \n", " ").replace('\n', ' ');
    }

    private AstNode child(AstNode parent, int type) {
        for (AstNode child : parent.getChildren()) {
            if (child.getType() == type) return child;
        }
        return null;
    }

    private static String encode(String string) {
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

    private static String encode(char c) {
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

}
