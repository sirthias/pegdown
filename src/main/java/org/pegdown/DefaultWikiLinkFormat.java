package org.pegdown;

import org.pegdown.ast.WikiLinkNode;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Implements the standard way of turning wiki links titles into URLs:
 * 1. Replace blank with dashes
 * 2. URL encode
 * 3. Prepend with "./" and append with ".html"
 */
public class DefaultWikiLinkFormat implements WikiLinkFormat {

    public String url(WikiLinkNode node) {
        return "./" + encodeTitle(node) + ".html";
    }

    public String encodeTitle(WikiLinkNode node) {
        try {
            return URLEncoder.encode(node.getText().replace(' ', '-'), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException();
        }
    }
}
