package org.pegdown;

import org.pegdown.ast.VerbatimNode;

public interface VerbatimSerializer {
    static final String DEFAULT = "DEFAULT";

    void serialize(VerbatimNode node, Printer printer);
}
