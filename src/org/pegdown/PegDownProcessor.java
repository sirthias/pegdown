package org.pegdown;

import org.jetbrains.annotations.NotNull;
import org.parboiled.Parboiled;
import org.parboiled.ReportingParseRunner;
import org.parboiled.common.StringUtils;
import org.parboiled.google.base.Preconditions;
import org.parboiled.support.ParsingResult;

import java.util.HashMap;
import java.util.Map;

public class PegDownProcessor implements AstNodeType {

    private final MarkDownParser parser;
    private final Map<String, String> refLinkUrls = new HashMap<String, String>();
    private StringBuilder sb;

    public PegDownProcessor() {
        this(false);
    }

    @SuppressWarnings({"unchecked"})
    public PegDownProcessor(boolean enableExtensions) {
        Class<?> parserClass = enableExtensions ? ExtendedMarkDownParser.class : MarkDownParser.class;
        parser = Parboiled.createParser((Class<MarkDownParser>) parserClass);
    }

    public String toHtml(@NotNull String markDownSource) {
        ParsingResult<AstNode> result = parseDocument(markDownSource);
        sb = new StringBuilder();
        buildRefLinkUrls();
        print(result.parseTreeRoot.getValue());
        return sb.toString();
    }

    @SuppressWarnings({"ConstantConditions"})
    private void buildRefLinkUrls() {
        refLinkUrls.clear();
        for (AstNode refNode : parser.references) {
            AstNode urlNode = findChildNode(refNode, LINK_URL);
            Preconditions.checkState(urlNode != null);
            String key = printToString(refNode).toLowerCase();
            refLinkUrls.put(key, urlNode.text);
        }
    }

    protected ParsingResult<AstNode> parseDocument(@NotNull String markDownSource) {
        parser.references.clear();
        ParsingResult<AstNode> result = ReportingParseRunner.run(parser.Doc(), markDownSource);
        if (!result.matched) {
            String errorMessage = "Internal error";
            if (result.hasErrors()) errorMessage += ": " + result.parseErrors.get(0);
            throw new RuntimeException(errorMessage);
        }
        return result;
    }

    public void print(AstNode node) {
        switch (node.type) {
            case DEFAULT:
                printChildren(node);
                return;
            case APOSTROPHE:
                sb.append("&rsquo;");
                return;
            case BLOCKQUOTE:
                printChildren(node, "\n\n<blockquote>", "</blockquote>");
                return;
            case CODE:
                printChildren(node, "<code>", "</code>");
                return;
            case ELLIPSIS:
                sb.append("&hellip;");
                return;
            case EMDASH:
                sb.append("&mdash;");
                return;
            case ENDASH:
                sb.append("&ndash;");
                return;
            case EMPH:
                printChildren(node, "<em>", "</em>");
                return;
            case H1:
                printChildren(node, "\n\n<h1>", "</h1>");
                return;
            case H2:
                printChildren(node, "\n\n<h2>", "</h2>");
                return;
            case H3:
                printChildren(node, "\n\n<h3>", "</h3>");
                return;
            case H4:
                printChildren(node, "\n\n<h4>", "</h4>");
                return;
            case H5:
                printChildren(node, "\n\n<h5>", "</h5>");
                return;
            case H6:
                printChildren(node, "\n\n<h6>", "</h6>");
                return;
            case HRULE:
                sb.append("\n\n<hr/>");
                return;
            case HTML:
                sb.append(node.text);
                return;
            case HTMLBLOCK:
                sb.append("\n\n").append(node.text);
                return;
            case IMAGE:
                printImage(node);
                return;
            case LINEBREAK:
                sb.append("<br/>\n");
                return;
            case LIST_BULLET:
                printListBullet(node);
                return;
            case LIST_ORDERED:
                printListOrdered(node);
                return;
            case LISTITEM_LOOSE:
                printListItemLoose(node);
                return;
            case LISTITEM_TIGHT:
                printListItemTight(node);
                return;
            case LISTITEMBLOCK:
                printListItemBlock(node);
                return;
            case LINK:
                printLink(node);
                return;
            case LINK_REF:
            case LINK_TITLE:
            case LINK_URL:
                return;
            case NOTE:
                printNote(node);
                return;
            case PARA:
                printChildren(node, "\n\n<p>", "</p>");
                return;
            case PLAIN:
                printChildren(node);
                return;
            case QUOTED_SINGLE:
                printChildren(node, "&lsquo;", "&rsquo;");
                return;
            case QUOTED_DOUBLE:
                printChildren(node, "&ldquo;", "&rdquo;");
                return;
            case REFERENCE:
                return;
            case SPACE:
                sb.append(node.text);
                return;
            case SPECIAL:
                printSpecial(node);
                return;
            case STRONG:
                printChildren(node, "<strong>", "</strong>");
                return;
            case TEXT:
                sb.append(node.text);
                return;
            case VERBATIM:
                printChildren(node, "\n\n<pre><code>", "</code></pre>");
                return;
        }
        throw new IllegalStateException();
    }

    private void printImage(AstNode node) {
        sb.append("<img src=\"");
        printLinkUrl(node);
        sb.append("\" alt=\"");
        printLinkTitle(node);
        sb.append('>');
        printChildren(node);
        sb.append("</img>");
    }

    private void printListBullet(AstNode node) {
    }

    private void printListOrdered(AstNode node) {
    }

    private void printListItemLoose(AstNode node) {
    }

    private void printListItemTight(AstNode node) {
    }

    private void printListItemBlock(AstNode node) {
    }

    private void printLink(AstNode node) {
        sb.append("<a href=\"");
        printLinkUrl(node);
        sb.append('"');
        printLinkTitle(node);
        sb.append('>');
        printChildren(node);
        sb.append("</a>");
    }

    private void printLinkUrl(AstNode node) {
        AstNode linkUrlNode = findChildNode(node, LINK_URL);
        if (linkUrlNode != null) {
            // explicit link
            sb.append(linkUrlNode.text);
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

    private void printNote(AstNode node) {
    }

    private void printSpecial(AstNode node) {
        char c = node.text.charAt(0);
        switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            default:
                sb.append(c);
                break;
        }
    }

    private void printChildren(AstNode node, String open, String close) {
        sb.append(open);
        printChildren(node);
        sb.append(close);
    }

    private void printChildren(AstNode node) {
        for (AstNode child : node.getChildren()) {
            print(child);
        }
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

}
