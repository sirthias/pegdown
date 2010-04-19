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
    private final Map<String, String> refLinkTitles = new HashMap<String, String>();
    private StringBuilder sb;
    private int indent = 0;

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
        ParsingResult<AstNode> result = parser.parseRawBlock(markDownSource + '\n');

        sb = new StringBuilder();
        buildRefLinkUrls();

        print(result.parseTreeRoot.getValue()).println();

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
            refLinkUrls.put(key, value);

            AstNode titleNode = findChildNode(refNode, LINK_TITLE);
            if (titleNode != null) {
                String title = encode(titleNode.text);
                refLinkTitles.put(key, title);
            }
        }
    }

    private PegDownProcessor print(AstNode node) {
        switch (node.type) {
            case DEFAULT:
                return printChildren(node);
            case APOSTROPHE:
                return print("&rsquo;");
            case BLOCKQUOTE:
                return printOnNL("<blockquote>").indent(+2).printChildren(node).indent(-2).printOnNL("</blockquote>");
            case CODE:
                return print("<code>").printEncoded(node.text).print("</code>");
            case ELLIPSIS:
                return print("&hellip;");
            case EMDASH:
                return print("&mdash;");
            case ENDASH:
                return print("&ndash;");
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
            case HRULE:
                return printOnNL("<hr/>").println();
            case HTML:
                return print(node.text);
            case HTMLBLOCK:
                return printOnNL(node.text).println();
            case IMAGE:
                return print("<img ").printLinkAttrs(node, "src", "alt").print(">").printLinkName(node).print("</img>");
            case LINEBREAK:
                return printOnNL("<br/>").println();
            case LIST_BULLET:
                return printOnNL("<ul>").indent(+2).printChildren(node).indent(-2).printOnNL("</ul>");
            case LIST_ORDERED:
                return printOnNL("<ol>").indent(+2).printChildren(node).indent(-2).printOnNL("</ol>");
            case LISTITEM:
                return printOnNL("<li>").printChildren(node).print("</li>");
            case LINK:
                return print("<a ").printLinkAttrs(node, "href", "title").print(">").printLinkName(node).print("</a>");
            case LINK_REF:
            case LINK_TITLE:
            case LINK_URL:
                return this;
            case NOTE:
                return printNote(node);
            case PARA:
                return printOnNL("<p>").printChildren(node).print("</p>");
            case QUOTED_SINGLE:
                return print("&lsquo;").printChildren(node).print("&rsquo;");
            case QUOTED_DOUBLE:
                return print("&ldquo;").printChildren(node).print("&rdquo;");
            case REFERENCE:
                return this;
            case SPACE:
                return print(node.text);
            case SPECIAL:
                return printSpecial(node);
            case STRONG:
                return print("<strong>").printChildren(node).print("</strong>");
            case TEXT:
                return print(node.text);
            case VERBATIM:
                return printOnNL("<pre><code>").printEncoded(node.text).print("</code></pre>");
        }
        throw new IllegalStateException();
    }

    private PegDownProcessor indent(int delta) {
        indent += delta;
        return this;
    }

    private PegDownProcessor print(String string) {
        sb.append(string);
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

    private PegDownProcessor printOnNL(String string) {
        println();
        printIndent();
        print(string);
        return this;
    }

    private PegDownProcessor printEncoded(String string) {
        return print(encode(string));
    }

    private PegDownProcessor printLinkAttrs(AstNode node, String hrefAttr, String titleAttr) {
        AstNode linkUrlNode = findChildNode(node, LINK_URL);
        if (linkUrlNode != null) {
            // explicit link
            print(hrefAttr).print("=\"").printEncoded(linkUrlNode.text).print('"');

            AstNode linkTitleNode = findChildNode(node, LINK_TITLE);
            return linkTitleNode == null ? this :
                    print(' ').print(titleAttr).print("=\"").printChildren(linkTitleNode).print('"');
        }

        // reference link
        AstNode linkRefNode = findChildNode(node, LINK_REF);
        String linkRef = linkRefNode == null ? "" : printToString(linkRefNode).toLowerCase();
        if (StringUtils.isEmpty(linkRef)) {
            // implicit reference link
            linkRef = printToString(node).toLowerCase();
        }

        String linkUrl = refLinkUrls.get(linkRef);
        print(hrefAttr).print("=\"").print(linkUrl == null ? "Undefined Link Reference!" : linkUrl).print('"');

        String linkTitle = refLinkTitles.get(linkRef);
        return linkTitle == null ? this : print(' ').print(titleAttr).print("=\"").print(linkTitle).print('"');
    }

    private PegDownProcessor printLinkName(AstNode node) {
        return StringUtils.isEmpty(node.text) ? printChildren(node) : printEncoded(node.text);
    }

    private PegDownProcessor printSpecial(AstNode node) {
        char c = node.text.charAt(0);
        String encoded = encode(c);
        return encoded != null ? print(encoded) : print(c);
    }

    private PegDownProcessor printNote(AstNode node) {
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
