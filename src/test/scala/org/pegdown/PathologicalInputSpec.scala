package org.pegdown

import Extensions._


class PathologicalInputSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {

    "properly parse pathological input example 1" in {
      // this test took about 30 seconds to complete in 0.8.5.4
      new PegDownProcessor(SMARTYPANTS | AUTOLINKS, 1000).markdownToHtml {
        """<table border>
          |<tr> <th>Your action <th>Partner's action <th>Your jail time <th>Partner's jail time
          |<tr> <td>silent      <td>silent           <td>1              <td>1
          |<tr> <td>silent      <td>confess          <td>5              <td>0
          |<tr> <td>confess     <td>silent           <td>0              <td>5
          |</table>
          |""".stripMargin
      } mustNotEqual null
    }

    "properly parse pathological input example 2" in {
      // this test took about 30 seconds to complete in 1.0.2
      new PegDownProcessor(SMARTYPANTS | AUTOLINKS, 1000).markdownToHtml {
        "***a*** ***b*** ***c*** ***d*** ***e*** ***f*** ***g*** ***h*** ***i*** ***f*** " +
        "***g*** ***h*** ***i*** ***j*** ***k*** ***l*** ***m*** ***n*** ***o*** ***p***"
      } mustNotEqual null
    }

    "properly parse pathological input example 3" in {
      new PegDownProcessor(200l).markdownToHtml {
        "how about a new method thats getObjectIdOrAdjustmentGroup? That w[a[[[[[[[[[[[[[[[[[y we're more explicit" +
          " and still benefit callers from having to do the iff dance"
      } must throwA[org.parboiled.errors.ParserRuntimeException]
    }

  }

}
