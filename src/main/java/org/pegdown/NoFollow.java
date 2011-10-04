package org.pegdown;

import org.pegdown.ast.AutoLinkNode;
import org.pegdown.ast.ExpLinkNode;
import org.pegdown.ast.RefLinkNode;
import org.pegdown.ast.WikiLinkNode;

public interface NoFollow {

    /**
     * Determines whether the given link node should receive a "rel=nofollow" attribute when generated as HTML.
     * @param node
     * @return true if the given link node should receive a "rel=nofollow" attribute
     */
    boolean noFollow(AutoLinkNode node);

    /**
     * Determines whether the given link node should receive a "rel=nofollow" attribute when generated as HTML.
     * @param node
     * @return true if the given link node should receive a "rel=nofollow" attribute
     */
    boolean noFollow(ExpLinkNode node);

    /**
     * Determines whether the given link node should receive a "rel=nofollow" attribute when generated as HTML.
     * @param node
     * @return true if the given link node should receive a "rel=nofollow" attribute
     */
    boolean noFollow(RefLinkNode node);

    /**
     * Determines whether the given link node should receive a "rel=nofollow" attribute when generated as HTML.
     * @param node
     * @return true if the given link node should receive a "rel=nofollow" attribute
     */
    boolean noFollow(WikiLinkNode node);

    static NoFollow NEVER = new NoFollow() {
        public boolean noFollow(AutoLinkNode node) { return false; }
        public boolean noFollow(ExpLinkNode node) { return false; }
        public boolean noFollow(RefLinkNode node) { return false; }
        public boolean noFollow(WikiLinkNode node) { return false; }
    };

    static NoFollow ALWAYS = new NoFollow() {
        public boolean noFollow(AutoLinkNode node) { return true; }
        public boolean noFollow(ExpLinkNode node) { return true; }
        public boolean noFollow(RefLinkNode node) { return true; }
        public boolean noFollow(WikiLinkNode node) { return true; }
    };
}
