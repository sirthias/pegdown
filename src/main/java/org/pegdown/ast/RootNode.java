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

package org.pegdown.ast;

import org.parboiled.common.ImmutableList;

import java.util.List;

import static org.parboiled.common.Preconditions.checkArgNotNull;

public class RootNode extends SuperNode {
    private List<ReferenceNode> references = ImmutableList.of();
    private List<AbbreviationNode> abbreviations = ImmutableList.of();

    public List<ReferenceNode> getReferences() {
        return references;
    }

    public void setReferences(List<ReferenceNode> references) {
        checkArgNotNull(references, "references");
        this.references = references;
    }

    public List<AbbreviationNode> getAbbreviations() {
        return abbreviations;
    }

    public void setAbbreviations(List<AbbreviationNode> abbreviations) {
        checkArgNotNull(abbreviations, "abbreviations");
        this.abbreviations = abbreviations;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
