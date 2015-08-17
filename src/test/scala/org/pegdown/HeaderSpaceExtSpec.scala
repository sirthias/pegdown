package org.pegdown

import org.pegdown.Extensions._


class HeaderSpaceExtSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {
    "pass all tests in the HeaderSpaceExt test suite" in {


      "header-space-ext" in {
        implicit val processor = new PegDownProcessor(ALL & ~HARDWRAPS)
        test("CompoundLists/plain-blockquote")
      }
      "no-header-space-ext" in {
        implicit val processor = new PegDownProcessor(ALL & ~HEADERSPACE & ~HARDWRAPS)
        test("CompoundLists/plain-blockquote")
      }
    }
  }

}
