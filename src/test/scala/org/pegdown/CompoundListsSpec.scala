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


    }
  }

}
