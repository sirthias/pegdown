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

      "escaped-chars-no-ext" in {
        implicit val processor = new PegDownProcessor(NONE)

        testAlt("OptionalExtensions/escaped-chars", "-no-ext")
      }
      "escaped-chars-all-ext" in {
        implicit val processor = new PegDownProcessor(ALL)
        testAlt("OptionalExtensions/escaped-chars", "-all-ext")
      }
      "hrules-not-relaxed" in {
        implicit val processor = new PegDownProcessor(ALL)
        testAlt("OptionalExtensions/hrules", "-not-relaxed")
        testASTAlt("OptionalExtensions/hrules", "-not-relaxed")
      }
      "hrules-relaxed" in {
        implicit val processor = new PegDownProcessor(ALL | RELAXEDHRULES)
        testAlt("OptionalExtensions/hrules", "-relaxed")
        testASTAlt("OptionalExtensions/hrules", "-relaxed")
      }
      "task-lists-no-ext" in {
        implicit val processor = new PegDownProcessor(ALL)
        testAlt("OptionalExtensions/task-lists", "-no-ext")
        testASTAlt("OptionalExtensions/task-lists", "-no-ext")
      }
      "task-lists-ext" in {
        implicit val processor = new PegDownProcessor(ALL | TASKLISTITEMS)
        testAlt("OptionalExtensions/task-lists", "-ext")
        testASTAlt("OptionalExtensions/task-lists", "-ext")
      }
    }
  }

}
