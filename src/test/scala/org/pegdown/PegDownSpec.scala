package org.pegdown

import ast.{Visitor, Node}
import org.parboiled.Parboiled
import Extensions._
import org.pegdown.ast.VerbatimNode
import org.parboiled.common.FileUtils
import java.util.Collections
import scala.collection.immutable.HashMap
import plugins.{ToHtmlSerializerPlugin, PegDownPlugins}


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

    "pass the custom verbatim-serializer test" in {
      def runWithSerializerMap(testName: String, verbatimSerializerMap: java.util.Map[String, VerbatimSerializer], suffix: String) {
        val expectedUntidy = FileUtils.readAllTextFromResource(testName + suffix + ".html")
        require(expectedUntidy != null, "Test '" + testName + "' not found")
        test(testName, tidy(expectedUntidy), new ToHtmlSerializer(new LinkRenderer, verbatimSerializerMap))(new PegDownProcessor(FENCED_CODE_BLOCKS))
      }
      "without specifying default" in {
        runWithSerializerMap("pegdown/GFM_Fenced_Code_Blocks", Collections.singletonMap("scala", new CustomVerbatimSerializer), "_reversed_scala")
      }
      "with specifying default" in {
        val serializerMap = new java.util.HashMap[String, VerbatimSerializer]
        serializerMap.put(VerbatimSerializer.DEFAULT, new CustomVerbatimSerializer)
        runWithSerializerMap("pegdown/GFM_Fenced_Code_Blocks", serializerMap, "_reversed_all")
      }
    }

    "allow custom plugins" in {
      import scala.collection.JavaConversions._
      implicit val processor = new PegDownProcessor(Parboiled.createParser[Parser, AnyRef](classOf[Parser],
        new java.lang.Integer(ALL), new java.lang.Long(1000), Parser.DefaultParseRunnerProvider,
        PegDownPlugins.builder().withPlugin(classOf[PluginParser]).build()))
      implicit val htmlSerializer = new ToHtmlSerializer(new LinkRenderer, List(new ToHtmlSerializerPlugin {
        def visit(node: Node, visitor: Visitor, printer: Printer) = node match {
          case blockPlugin: BlockPluginNode => {
            printer.print("<div class=\"blockplugin\">")
            printer.print(blockPlugin.getText)
            printer.print("</div>")
            true
          }
          case inlinePlugin: InlinePluginNode => {
            printer.print("<span class=\"inlineplugin\">")
            printer.print(inlinePlugin.getText)
            printer.print("</span>")
            true
          }
          case _ => false
        }
      }))

      testWithSerializer("pegdown/Plugins")
    }
  }

}

class CustomParser extends Parser(ALL, 1000, Parser.DefaultParseRunnerProvider)

class CustomVerbatimSerializer extends VerbatimSerializer {
  def serialize(node: VerbatimNode, printer: Printer) {
    printer.print("<pre>")
    printer.println()
    printer.print("<code class=\"reversed-" + node.getType + "\">")
    printer.println()
    printer.print(node.getText.reverse)
    printer.println()
    printer.print("</code>")
    printer.println()
    printer.print("</pre>")
    printer.println()
  }
}
