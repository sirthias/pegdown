package org.pegdown

import Extensions._


class PhpMarkdownSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {

    "pass selected parts of the PhpMarkdown test suite" in {
      implicit val processor = new PegDownProcessor

      test("PhpMarkdown/Backslash_escapes")
      test("PhpMarkdown/Code_block_in_a_list_item")
      test("PhpMarkdown/Code_Spans")
      test("PhpMarkdown/Email_auto_links")
//      test("PhpMarkdown/Emphasis")
//      test("PhpMarkdown/Headers")
      test("PhpMarkdown/Horizontal_Rules")
      test("PhpMarkdown/Inline_HTML_(Simple)")
      test("PhpMarkdown/Inline_HTML_(Span)")
      test("PhpMarkdown/Inline_HTML_comments")
//      test("PhpMarkdown/Ins_and_del")
//      test("PhpMarkdown/Links_inline_style")
      test("PhpMarkdown/MD5_Hashes")
      test("PhpMarkdown/Nesting")
//      test("PhpMarkdown/Parens_in_URL")
//      test("PhpMarkdown/PHP-Specific_Bugs")
      test("PhpMarkdown/Tight_blocks")
    }

    "pass selected parts of the PhpMarkdownExtra test suite" in {
      implicit val processor = new PegDownProcessor(ALL & ~SMARTYPANTS & ~HARDWRAPS & ~ANCHORLINKS)

      test("PhpMarkdownExtra/Abbr")
      test("PhpMarkdownExtra/Definition_Lists")
      test("PhpMarkdownExtra/Fenced_Code_Blocks")
      test("PhpMarkdownExtra/Tables")
    }
  }

}
