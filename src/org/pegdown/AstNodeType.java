package org.pegdown;

public interface AstNodeType {

    static int NONE = 0;
    static int APOSTROPHE = 1;
    static int BLOCKQUOTE = 2;
    static int CODE = 3;
    static int ELLIPSIS = 4;
    static int EMDASH = 5;
    static int ENDASH = 6;
    static int EMPH = 7;
    static int H1 = 8;
    static int H2 = 9;
    static int H3 = 10;
    static int H4 = 11;
    static int H5 = 12;
    static int H6 = 13;
    static int HRULE = 14;
    static int HTML = 15;
    static int HTMLBLOCK = 16;
    static int IMAGE = 17;
    static int LINEBREAK = 18;
    static int LIST_BULLET = 19;
    static int LIST_ORDERED = 20;
    static int LISTITEM_TIGHT = 21;
    static int LISTITEM_LOOSE = 22;
    static int LISTITEMBLOCK = 23;
    static int LINK = 24;
    static int NOTE = 25;
    static int PARA = 26;
    static int PLAIN = 27;
    static int QUOTED_SINGLE = 28;
    static int QUOTED_DOUBLE = 29;
    static int REFERENCE = 30;
    static int SPACE = 31;
    static int STR = 32;
    static int STRONG = 33;
    static int VERBATIM = 34;

    static String[] TYPE_NAMES = new String[]{
            "NONE",
            "APOSTROPHE",
            "BLOCKQUOTE",
            "CODE",
            "ELLIPSIS",
            "EMDASH",
            "ENDASH",
            "EMPH",
            "H1", "H2", "H3", "H4", "H5", "H6",
            "HRULE",
            "HTML",
            "HTMLBLOCK",
            "IMAGE",
            "LINEBREAK",
            "LIST_BULLET",
            "LIST_ORDERED",
            "LISTITEM_TIGHT",
            "LISTITEM_LOOSE",
            "LISTITEMBLOCK",
            "LINK",
            "NOTE",
            "PARA",
            "PLAIN",
            "QUOTED_SINGLE",
            "QUOTED_DOUBLE",
            "REFERENCE",
            "SPACE",
            "STR",
            "STRONG",
            "VERBATIM"
    };

}
