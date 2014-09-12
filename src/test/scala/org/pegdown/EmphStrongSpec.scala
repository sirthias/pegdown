package org.pegdown

import Extensions._


class EmphStrongSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {
    "pass all tests in the EmphStrong test suite" in {
      implicit val processor = new PegDownProcessor(ALL & ~ANCHORLINKS)

      test("emph-strong-test/test_1")
      test("emph-strong-test/test_2")
      test("emph-strong-test/test_3")
      test("emph-strong-test/test_4")
      test("emph-strong-test/test_5")
      test("emph-strong-test/test_6")
      test("emph-strong-test/test_7")
      test("emph-strong-test/test_8")
      test("emph-strong-test/test_9")
      test("emph-strong-test/test_10")
      test("emph-strong-test/test_11")
      test("emph-strong-test/test_12")
      test("emph-strong-test/test_13")
      test("emph-strong-test/test_14")
      test("emph-strong-test/test_15")
      test("emph-strong-test/test_16")
      test("emph-strong-test/test_17")
      test("emph-strong-test/test_18")
      test("emph-strong-test/test_19")
      test("emph-strong-test/test_20")
      test("emph-strong-test/test_21")
      test("emph-strong-test/test_22")
    }
  }

}
