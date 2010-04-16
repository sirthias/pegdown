package org.pegdown;

import org.jetbrains.annotations.NotNull;
import org.parboiled.Parboiled;
import org.parboiled.common.StringUtils;
import org.parboiled.google.base.Preconditions;
import org.parboiled.support.ParsingResult;

import java.util.HashMap;
import java.util.Map;

public class PegDownProcessor implements AstNodeType {

    private final MarkDownParser parser;
    private final Map<String, String> refLinkUrls = new HashMap<String, String>();
    private StringBuilder sb;
    private String padding;

    public PegDownProcessor() {
        this(false);
    }

    @SuppressWarnings({"unchecked"})
    public PegDownProcessor(boolean enableExtensions) {
        Class<?> parserClass = enableExtensions ? ExtendedMarkDownParser.class : MarkDownParser.class;
        parser = Parboiled.createParser((Class<MarkDownParser>) parserClass);
    }

    public MarkDownParser getParser() {
        return parser;
    }

    public String toHtml(@NotNull String markDownSource) {
        parser.references.clear();
        ParsingResult<AstNode> result = parser.parseRawBlock(markDownSource);

        sb = new StringBuilder();
        buildRefLinkUrls();

        setPadding(0).print(result.parseTreeRoot.getValue());

        sb.append('\n'); // add final newline
        return sb.toString();
    }

    @SuppressWarnings({"ConstantConditions"})
    private void buildRefLinkUrls() {
        refLinkUrls.clear();
        for (AstNode refNode : parser.references) {
            AstNode urlNode = findChildNode(refNode, LINK_URL);
            Preconditions.checkState(urlNode != null);
            String key = printToString(refNode).toLowerCase();
            String value = encode(urlNode.text);
            AstNode titleNode = findChildNode(refNode, LINK_TITLE);
            if (titleNode != null) {
                value += "\" title=\"" + encode(titleNode.text);
            }
            refLinkUrls.put(key, value);
        }
    }

    private PegDownProcessor setPadding(int count) {
        switch (count) {
            case 0:
                padding = "";
                break;
            case 1:
                padding = "\n";
                break;
            case 2:
                padding = "\n\n";
                break;
            default:
                throw new IllegalStateException();
        }
        return this;
    }

    private PegDownProcessor printPadding() {
        sb.append(padding);
        return this;
    }

    private PegDownProcessor print(String string) {
        sb.append(string);
        return this;
    }

    private PegDownProcessor print(AstNode node) {
        switch (node.type) {
            case DEFAULT:
                return printChildren(node);
            case APOSTROPHE:
                return print("&rsquo;");
            case BLOCKQUOTE:
                return printPadding().setPadding(0).printChildren(node, "<blockquote>\n  ", "\n</blockquote>");
            case CODE:
                return print("<code>").print(encode(node.text)).print("</code>");
            case ELLIPSIS:
                return print("&hellip;");
            case EMDASH:
                return print("&mdash;");
            case ENDASH:
                return print("&ndash;");
            case EMPH:
                return printChildren(node, "<em>", "</em>");
            case H1:
                return printChildren(node, "<h1>", "</h1>");
            case H2:
                return printChildren(node, "<h2>", "</h2>");
            case H3:
                return printChildren(node, "<h3>", "</h3>");
            case H4:
                return printChildren(node, "<h4>", "</h4>");
            case H5:
                return printChildren(node, "<h5>", "</h5>");
            case H6:
                return printChildren(node, "<h6>", "</h6>");
            case HRULE:
                return printPadding().print("<hr/>");
            case HTML:
                return print(node.text);
            case HTMLBLOCK:
                return printPadding().print(node.text);
            case IMAGE:
                return printImage(node);
            case LINEBREAK:
                return print("<br/>\n");
            case LIST_BULLET:
                return printPadding().printChildren(node, "<ul>", "\n</ul>");
            case LIST_ORDERED:
                return printChildren(node, "<ol>", "\n</ol>");
            case LISTITEM:
                return printChildren(node, "\n<li>", "</li>");
            case LINK:
                return printLink(node);
            case LINK_REF:
            case LINK_TITLE:
            case LINK_URL:
                return this;
            case NOTE:
                return printNote(node);
            case PARA:
                return printPadding().printChildren(node, "<p>", "</p>").setPadding(2);
            case QUOTED_SINGLE:
                return printChildren(node, "&lsquo;", "&rsquo;");
            case QUOTED_DOUBLE:
                return printChildren(node, "&ldquo;", "&rdquo;");
            case REFERENCE:
                return this;
            case SPACE:
                return print(node.text);
            case SPECIAL:
                return printSpecial(node);
            case STRONG:
                return printChildren(node, "<strong>", "</strong>");
            case TEXT:
                return print(node.text);
            case VERBATIM:
                return printPadding().print("<pre><code>").print(encode(node.text)).print("</code></pre>");
        }
        throw new IllegalStateException();
    }

    private PegDownProcessor printImage(AstNode node) {
        sb.append("<img src=\"");
        printLinkUrl(node);
        sb.append("\" alt=\"");
        printLinkTitle(node);
        sb.append('>');
        printLinkName(node);
        return print("</img>");
    }

    private PegDownProcessor printLink(AstNode node) {
        sb.append("<a href=\"");
        printLinkUrl(node);
        sb.append('"');
        printLinkTitle(node);
        sb.append('>');
        printLinkName(node);
        return print("</a>");
    }

    private void printLinkUrl(AstNode node) {
        AstNode linkUrlNode = findChildNode(node, LINK_URL);
        if (linkUrlNode != null) {
            // explicit link
            sb.append(encode(linkUrlNode.text));
            return;
        }

        // reference link
        AstNode linkRefNode = findChildNode(node, LINK_REF);
        String linkRef = linkRefNode == null ? "" : printToString(linkRefNode).toLowerCase();
        if (StringUtils.isEmpty(linkRef)) {
            // implicit reference link
            linkRef = printToString(node).toLowerCase();
        }

        String linkUrl = refLinkUrls.get(linkRef);
        sb.append(linkUrl == null ? "Undefined Link Reference!" : linkUrl);
    }

    private void printLinkTitle(AstNode node) {
        AstNode linkTitleNode = findChildNode(node, LINK_TITLE);
        if (linkTitleNode != null) {
            sb.append(" title=\"");
            printChildren(linkTitleNode);
            sb.append('\'');
        }
    }

    private void printLinkName(AstNode node) {
        if (StringUtils.isEmpty(node.text)) {
            printChildren(node);
        } else {
            sb.append(encode(node.text));
        }
    }

    private PegDownProcessor printSpecial(AstNode node) {
        char c = node.text.charAt(0);
        String encoded = encode(c);
        if (encoded != null) {
            sb.append(encoded);
        } else {
            sb.append(c);
        }
        return this;
    }

    private PegDownProcessor printNote(AstNode node) {
        return this;
    }

    private PegDownProcessor printChildren(AstNode node, String open, String close) {
        sb.append(open);
        printChildren(node);
        sb.append(close);
        return this;
    }

    private PegDownProcessor printChildren(AstNode node) {
        for (AstNode child : node.getChildren()) {
            print(child);
        }
        return this;
    }

    private String printToString(AstNode node) {
        int len = sb.length();
        printChildren(node);
        if (sb.length() == len) return "";
        String text = sb.substring(len, sb.length());
        sb.setLength(len);
        return text;
    }

    private AstNode findChildNode(AstNode parent, int type) {
        for (AstNode node : parent.getChildren()) {
            if (node.type == type) return node;
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
                    if (encoded != null) sb.append(encoded);
                    else sb.append(c);
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
