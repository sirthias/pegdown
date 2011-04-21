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

package org.pegdown.ast;

public class RefLinkNode extends SuperNode {
    private String separatorSpace;
    private SuperNode referenceKey;
    private boolean image;

    public RefLinkNode(Node child) {
        super(child);
    }

    public String getSeparatorSpace() {
        return separatorSpace;
    }

    public boolean setSeparatorSpace(String separatorSpace) {
        this.separatorSpace = separatorSpace;
        return true;
    }

    public SuperNode getReferenceKey() {
        return referenceKey;
    }

    public boolean setReferenceKey(SuperNode referenceKey) {
        this.referenceKey = referenceKey;
        return true;
    }

    public boolean getImage() {
		return image;
	}

    public boolean makeImage() {
        return image = true;
    }
    
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}