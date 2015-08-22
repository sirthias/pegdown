package org.pegdown

import org.pegdown.Extensions._


class CompoundListsSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {
    "pass all tests in the CompoundList test suite" in {

      "plain-blockquote" in {
        implicit val processor = new PegDownProcessor(ALL & ~HARDWRAPS)
        test("CompoundLists/plain-blockquote")
      }
      "plain-header" in {
        implicit val processor = new PegDownProcessor(ALL)
        test("CompoundLists/plain-header")
      }
      "plain-para" in {
        implicit val processor = new PegDownProcessor(ALL)
        test("CompoundLists/plain-para")
      }
      "plain-setext-header" in {
        implicit val processor = new PegDownProcessor(ALL)
        test("CompoundLists/plain-setext-header")
      }
      "loose-sublist-singleitem" in {
        implicit val processor = new PegDownProcessor(ALL)
        test("CompoundLists/loose-sublist-singleitem")
      }
      "loose-shallow" in {
        implicit val processor = new PegDownProcessor(ALL)
        test("CompoundLists/loose-shallow")
      }
      "nested-one-level" in {
        implicit val processor = new PegDownProcessor(ALL)
        test("CompoundLists/nested-one-level")
      }
      "nested-two-levels" in {
        implicit val processor = new PegDownProcessor(ALL)
        test("CompoundLists/nested-two-levels")
      }
      "shallow" in {
        implicit val processor = new PegDownProcessor(ALL)
        test("CompoundLists/shallow")
      }

      // force list item para wrapping if it includes more than just simple para
      "plain-blockquote-forcepara" in {
        implicit val processor = new PegDownProcessor((ALL & ~HARDWRAPS) | FORCELISTITEMPARA)
        testAlt("CompoundLists/plain-blockquote", "-forcepara")
      }
      "plain-header-forcepara" in {
        implicit val processor = new PegDownProcessor(ALL | FORCELISTITEMPARA)
        testAlt("CompoundLists/plain-header", "-forcepara")
      }
      "plain-para-forcepara" in {
        implicit val processor = new PegDownProcessor(ALL | FORCELISTITEMPARA)
        testAlt("CompoundLists/plain-para", "-forcepara")
      }
      "plain-setext-header-forcepara" in {
        implicit val processor = new PegDownProcessor(ALL | FORCELISTITEMPARA)
        testAlt("CompoundLists/plain-setext-header", "-forcepara")
      }
      "loose-sublist-singleitem-forcepara" in {
        implicit val processor = new PegDownProcessor(ALL | FORCELISTITEMPARA)
        testAlt("CompoundLists/loose-sublist-singleitem", "-forcepara")
      }
      "loose-shallow-forcepara" in {
        implicit val processor = new PegDownProcessor(ALL | FORCELISTITEMPARA)
        testAlt("CompoundLists/loose-shallow", "-forcepara")
      }
      "nested-one-level-forcepara" in {
        implicit val processor = new PegDownProcessor(ALL | FORCELISTITEMPARA)
        testAlt("CompoundLists/nested-one-level", "-forcepara")
      }
      "nested-two-levels-forcepara" in {
        implicit val processor = new PegDownProcessor(ALL | FORCELISTITEMPARA)
        testAlt("CompoundLists/nested-two-levels", "-forcepara")
      }
      "shallow-forcepara" in {
        implicit val processor = new PegDownProcessor(ALL | FORCELISTITEMPARA)
        testAlt("CompoundLists/shallow", "-forcepara")
      }

    }
  }

}
