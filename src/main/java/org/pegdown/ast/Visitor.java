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

public interface Visitor {
    void visit(AbbreviationNode node);
    void visit(AutoLinkNode node);
    void visit(BlockQuoteNode node);
    void visit(BulletListNode node);
    void visit(CodeNode node);
    void visit(DefinitionListNode node);
    void visit(DefinitionNode node);
    void visit(DefinitionTermNode node);
    void visit(ExpImageNode node);
    void visit(ExpLinkNode node);
    void visit(HeaderNode node);
    void visit(HtmlBlockNode node);
    void visit(InlineHtmlNode node);
    void visit(ListItemNode node);
    void visit(MailLinkNode node);
    void visit(OrderedListNode node);
    void visit(ParaNode node);
    void visit(QuotedNode node);
    void visit(ReferenceNode node);
    void visit(RefImageNode node);
    void visit(RefLinkNode node);
    void visit(RootNode node);
    void visit(SimpleNode node);
    void visit(SpecialTextNode node);
    void visit(StrikeNode node);
    void visit(StrongEmphSuperNode node);
    void visit(TableBodyNode node);
    void visit(TableCaptionNode node);
    void visit(TableCellNode node);
    void visit(TableColumnNode node);
    void visit(TableHeaderNode node);
    void visit(TableNode node);
    void visit(TableRowNode node);
    void visit(VerbatimNode node);
    void visit(WikiLinkNode node);

    void visit(TextNode node);
    void visit(SuperNode node);
    void visit(Node node); // general catch all for custom Node implementations    
}
