package org.pegdown

import Extensions._


class PathologicalInputSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {

    "properly parse pathological input example 1" in {
      // this test took about 30 seconds to complete in 0.8.5.4
      val time = System.currentTimeMillis
      new PegDownProcessor(SMARTYPANTS | AUTOLINKS).markdownToHtml {
        """<table border>
          |<tr> <th>Your action <th>Partner's action <th>Your jail time <th>Partner's jail time
          |<tr> <td>silent      <td>silent           <td>1              <td>1
          |<tr> <td>silent      <td>confess          <td>5              <td>0
          |<tr> <td>confess     <td>silent           <td>0              <td>5
          |</table>
          |""".stripMargin
      }
      (System.currentTimeMillis - time) must be_< (1000L)
    }

    "properly parse pathological input example 2" in {
      // this test took about 30 seconds to complete in 1.0.2
      val time = System.currentTimeMillis
      new PegDownProcessor(SMARTYPANTS | AUTOLINKS).markdownToHtml {
        "***a*** ***b*** ***c*** ***d*** ***e*** ***f*** ***g*** ***h*** ***i*** ***f*** " +
        "***g*** ***h*** ***i*** ***j*** ***k*** ***l*** ***m*** ***n*** ***o*** ***p***"
      }
      (System.currentTimeMillis - time) must be_< (1000L)
    }

  }

}
