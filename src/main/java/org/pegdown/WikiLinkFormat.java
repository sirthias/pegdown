package org.pegdown;

import org.pegdown.ast.WikiLinkNode;

/**
 * Encapsulates the logic that turn a wiki link title into a link URL.
 */
public interface WikiLinkFormat {

    /**
     * Returns the URL for the link with the given title.
     *
     * @param node the WikiLinkNode to get the URL for
     * @return the url
     */
    String url(WikiLinkNode node);


    static WikiLinkFormat DEFAULT = new DefaultWikiLinkFormat();
}
