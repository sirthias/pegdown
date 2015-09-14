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
      }
      "hrules-not-relaxed AST" in {
        implicit val processor = new PegDownProcessor(ALL)
        testASTAlt("OptionalExtensions/hrules", "-not-relaxed")
      }
      "hrules-relaxed" in {
        implicit val processor = new PegDownProcessor(ALL | RELAXEDHRULES)
        testAlt("OptionalExtensions/hrules", "-relaxed")
      }
      "hrules-relaxed AST" in {
        implicit val processor = new PegDownProcessor(ALL | RELAXEDHRULES)
        testASTAlt("OptionalExtensions/hrules", "-relaxed")
      }
      "task-lists-no-ext" in {
        implicit val processor = new PegDownProcessor(ALL)
        testAlt("OptionalExtensions/task-lists", "-no-ext")
      }
      "task-lists-no-ext AST" in {
        implicit val processor = new PegDownProcessor(ALL)
        testASTAlt("OptionalExtensions/task-lists", "-no-ext")
      }
      "task-lists-ext" in {
        implicit val processor = new PegDownProcessor(ALL | TASKLISTITEMS)
        testAlt("OptionalExtensions/task-lists", "-ext")
      }
      "task-lists-ext AST" in {
        implicit val processor = new PegDownProcessor(ALL | TASKLISTITEMS)
        testASTAlt("OptionalExtensions/task-lists", "-ext")
      }
      "extanchors-no-ext" in {
        implicit val processor = new PegDownProcessor(ALL)
        testAlt("OptionalExtensions/extanchors", "-no-ext")
      }
      "extanchors-no-ext AST" in {
        implicit val processor = new PegDownProcessor(ALL)
        testASTAlt("OptionalExtensions/extanchors", "-no-ext")
      }
      "extanchors-ext" in {
        implicit val processor = new PegDownProcessor(ALL | EXTANCHORLINKS)
        testAlt("OptionalExtensions/extanchors", "-ext")
      }
      "extanchors-ext AST" in {
        implicit val processor = new PegDownProcessor(ALL | EXTANCHORLINKS)
        testASTAlt("OptionalExtensions/extanchors", "-ext")
      }
    }
  }
}
