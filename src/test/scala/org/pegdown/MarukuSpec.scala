package org.pegdown

import Extensions._


class MarukuSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {
    "pass selected parts of the Maruku test suite" in {
      implicit val processor = new PegDownProcessor(ALL & ~HARDWRAPS)

      test("Maruku/abbreviations")
      test("Maruku/alt")
      test("Maruku/blank")
      test("Maruku/blanks_in_code")
      // test("Maruku/bug_def")
      // test("Maruku/bug_table")
      test("Maruku/code")
      test("Maruku/code2")
      test("Maruku/code3")
      test("Maruku/data_loss")
      test("Maruku/easy")
      test("Maruku/email")
      test("Maruku/entities")
      test("Maruku/escaping")
      // test("Maruku/extra_dl")
      // test("Maruku/extra_header_id")
      test("Maruku/extra_table1")
      // test("Maruku/footnotes")
      test("Maruku/headers")
      test("Maruku/hex_entities")
      // test("Maruku/hrule")
      // test("Maruku/html2")
      test("Maruku/html3")
      // test("Maruku/html4")
      // test("Maruku/html5")
      // test("Maruku/ie")
      test("Maruku/images")
      test("Maruku/images2")
      //test("Maruku/inline_html")
      //test("Maruku/inline_html2")
      test("Maruku/links")
      test("Maruku/list1")
      test("Maruku/list2")
      test("Maruku/list3")
      //test("Maruku/list4")
      test("Maruku/lists")
      //test("Maruku/lists11")
      test("Maruku/lists6")
      //test("Maruku/lists7")
      test("Maruku/lists7b")
      test("Maruku/lists8")
      //test("Maruku/lists9")
      //test("Maruku/lists_after_paragraph")
      test("Maruku/lists_ol")
      //test("Maruku/loss")
      //test("Maruku/misc_sw")
      test("Maruku/olist")
      test("Maruku/one")
      test("Maruku/paragraph")
      test("Maruku/paragraphs")
      test("Maruku/smartypants")
    }
  }

}
