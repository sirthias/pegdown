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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class WikiLinkNode
    extends TextNode {
    private boolean nofollow;
    private String url;

    public WikiLinkNode(String page, boolean nofollow) {
        super(page);
        String s;
        try {
            s = URLEncoder.encode(page.replace(' ', '-'), "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            s = page;
        }
        this.url = "./" + s + ".html";
        this.nofollow = nofollow;
    }

    public boolean isNofollow() {
        return nofollow;
    }

    public String getUrl()
    {
        return url;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}