package org.pegdown

import org.pegdown.Extensions._


class OptionalExtensionsSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {
    "pass all tests in the HeaderSpaceExt test suite" in {


      "header-space-ext" in {
        implicit val processor = new PegDownProcessor((ALL & ~HARDWRAPS) | ATXHEADERSPACE)

        test("OptionalExtensions/header-space-ext")
        test("OptionalExtensions/setext-headers")
      }

      "no-header-space-ext" in {
        implicit val processor = new PegDownProcessor(ALL & ~HARDWRAPS)

        test("OptionalExtensions/no-header-space-ext")
        test("OptionalExtensions/setext-headers")
      }

      "escaped chars with no-extensions" in {
        implicit val processor = new PegDownProcessor(NONE)

        testAlt("OptionalExtensions/escaped-chars", "-no-ext")
      }
      "escaped chars with all-extensions" in {
        implicit val processor = new PegDownProcessor(ALL)
        testAlt("OptionalExtensions/escaped-chars", "-all-ext")
      }
      "horizonal rules without RELAXEDHRULES" in {
        implicit val processor = new PegDownProcessor(ALL)
        testAlt("OptionalExtensions/hrules", "-not-relaxed")
        testASTAlt("OptionalExtensions/hrules", "-not-relaxed")
      }
      "horizonal rules with RELAXEDHRULES" in {
        implicit val processor = new PegDownProcessor(ALL | RELAXEDHRULES)
        testAlt("OptionalExtensions/hrules", "-relaxed")
        testASTAlt("OptionalExtensions/hrules", "-relaxed")
      }
    }
  }

}
