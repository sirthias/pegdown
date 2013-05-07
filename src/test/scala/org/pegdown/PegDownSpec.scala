package org.pegdown

import org.parboiled.Parboiled
import Extensions._


class PegDownSpec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {

    "pass the custom pegdown tests for all extensions" in {
      def runSuite(implicit processor: PegDownProcessor) {
        test("pegdown/Abbreviations")
        test("pegdown/AttributeWithUnderscore")
        test("pegdown/Autolinks")
        test("pegdown/Bug_in_0.8.5.1")
        test("pegdown/Bug_in_0.8.5.4")
        test("pegdown/Bug_in_1.0.0")
        test("pegdown/Bug_in_1.1.0")
        test("pegdown/GFM_Fenced_Code_Blocks")
        test("pegdown/Linebreaks")
        test("pegdown/Parens_in_URL")
        test("pegdown/Quoted Blockquote")
        test("pegdown/Smartypants")
        test("pegdown/Tables")
        test("pegdown/Wikilinks")

        testAST("pegdown/AstText")
        testAST("pegdown/GFM_Fenced_Code_Blocks")
      }

      "with the default parser" in {
        runSuite(new PegDownProcessor(ALL))
      }
      "with a custom parser" in {
        runSuite(new PegDownProcessor(Parboiled.createParser[CustomParser, AnyRef](classOf[CustomParser])))
      }
    }

    "pass the custom pegdown tests for no extensions" in {
      implicit val processor = new PegDownProcessor

      test("pegdown/Emph_With_Linebreaks")
      test("pegdown/Special Chars")
    }

    "pass the HTML suppression test" in {
      "without suppression" in {
        test("pegdown/HTML suppression",
          """<h1>HTML <b>SUPPRESSION</b></h1>
            |<p>This is a paragraph containing a <strong>strong</strong> inline
            |HTML element and:</p>
            |<div>
            |<p>an actual block of HTML!</p>
            |</div>
            |
            |""".stripMargin
        )(new PegDownProcessor)
      }
      "with inline suppression" in {
        test("pegdown/HTML suppression",
          """<h1>HTML SUPPRESSION</h1>
            |<p>This is a paragraph containing a strong inline HTML element
            |and:</p>
            |<div>
            |<p>an actual block of HTML!</p>
            |</div>
            |
            |""".stripMargin
        )(new PegDownProcessor(SUPPRESS_INLINE_HTML))
      }
      "with block suppression" in {
        test("pegdown/HTML suppression",
          """<h1>HTML <b>SUPPRESSION</b></h1>
            |<p>This is a paragraph containing a <strong>strong</strong> inline
            |HTML element and:</p>
            |
            |""".stripMargin
        )(new PegDownProcessor(SUPPRESS_HTML_BLOCKS))
      }
      "with block and inline suppression" in {
        test("pegdown/HTML suppression",
          """<h1>HTML SUPPRESSION</h1>
            |<p>This is a paragraph containing a strong inline HTML element
            |and:</p>
            |
            |""".stripMargin
        )(new PegDownProcessor(SUPPRESS_ALL_HTML))
      }
    }
  }

}

class CustomParser extends Parser(ALL, 1000, Parser.DefaultParseRunnerProvider)