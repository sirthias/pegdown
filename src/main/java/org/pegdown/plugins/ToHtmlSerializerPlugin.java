/*
 * Copyright (C) 2010-2011 Mathias Doenitz
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

package org.pegdown.plugins;

import org.pegdown.Printer;
import org.pegdown.ast.Node;
import org.pegdown.ast.Visitor;

/**
 * A plugin for the {@link org.pegdown.ToHtmlSerializer}
 */
public interface ToHtmlSerializerPlugin {

    /**
     * Visit the given node
     *
     * @param node The node to visit
     * @param visitor The visitor, for delegating back to handling children, etc
     * @param printer The printer to print output to
     * @return true if this plugin knew how to serialize the node, false otherwise
     */
    boolean visit(Node node, Visitor visitor, Printer printer);
}
