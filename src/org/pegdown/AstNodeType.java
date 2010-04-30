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

/**
 * Convenience interface defining the AST node type constants as well as a lookup array for reverse naming resolution. 
 */
public interface AstNodeType {

    static int DEFAULT = 0;

    // basic elements
    static int APOSTROPHE = 1;
    static int ELLIPSIS = 2;
    static int EMDASH = 3;
    static int ENDASH = 4;
    static int HTML = 5;
    static int LINEBREAK = 6;
    static int SPACE = 7;
    static int SPECIAL = 8;
    static int TEXT = 9;

    // inline groups
    static int CODE = 10;
    static int EMPH = 11;
    static int H1 = 12;
    static int H2 = 13;
    static int H3 = 14;
    static int H4 = 15;
    static int H5 = 16;
    static int H6 = 17;
    static int STRONG = 18;
    static int SINGLE_QUOTED = 19;
    static int DOUBLE_QUOTED = 20;

    // blocks
    static int BLOCKQUOTE = 21;
    static int HRULE = 22;
    static int HTMLBLOCK = 23;
    static int PARA = 24;
    static int VERBATIM = 25;

    // lists
    static int BULLET_LIST = 26;
    static int ORDERED_LIST = 27;
    static int TIGHT_LIST_ITEM = 28;
    static int LOOSE_LIST_ITEM = 29;

    // links
    static int AUTO_LINK = 30;
    static int EXP_LINK = 31;
    static int EXP_IMG_LINK = 32;
    static int MAIL_LINK = 33;
    static int REF_LINK = 34;
    static int REF_IMG_LINK = 35;

    static int LINK_LABEL = 36;
    static int LINK_REF = 37;
    static int LINK_TITLE = 38;
    static int LINK_URL = 39;
    static int REFERENCE = 40;

    // reverse resolution
    static String[] TYPE_NAMES = new String[]{
            "DEFAULT",
            "APOSTROPHE", "ELLIPSIS", "EMDASH", "ENDASH", "HTML", "LINEBREAK", "SPACE", "SPECIAL", "TEXT",
            "CODE", "EMPH", "H1", "H2", "H3", "H4", "H5", "H6", "STRONG", "SINGLE_QUOTED", "DOUBLE_QUOTED",
            "BLOCKQUOTE", "HRULE", "HTMLBLOCK", "PARA", "VERBATIM",
            "BULLET_LIST", "ORDERED_LIST", "TIGHT_LIST_ITEM", "LOOSE_LIST_ITEM",
            "AUTO_LINK", "EXP_LINK", "EXP_IMG_LINK", "MAIL_LINK", "REF_LINK", "REF_IMG_LINK",
            "LINK_LABEL", "LINK_REF", "LINK_TITLE", "LINK_URL", "REFERENCE"
    };

}
