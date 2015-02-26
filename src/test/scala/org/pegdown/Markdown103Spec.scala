package org.pegdown

import Extensions._


class Markdown103Spec extends AbstractPegDownSpec {

  "The PegDownProcessor" should {

    "pass the Markdown test suite" in {
      def runMarkdownTestSuite(implicit processor: PegDownProcessor) = {
        test("MarkdownTest103/Amps and angle encoding")
        test("MarkdownTest103/Auto links")
        test("MarkdownTest103/Backslash escapes")
        test("MarkdownTest103/Blockquotes with code blocks")
        test("MarkdownTest103/Code Blocks")
        test("MarkdownTest103/Code Spans")
        test("MarkdownTest103/Hard-wrapped paragraphs with list-like lines")
        test("MarkdownTest103/Horizontal rules")
        test("MarkdownTest103/Inline HTML (Advanced)")
        test("MarkdownTest103/Inline HTML (Simple)")
        test("MarkdownTest103/Inline HTML comments")
        test("MarkdownTest103/Links, inline style")
        test("MarkdownTest103/Links, reference style")
        test("MarkdownTest103/Links, shortcut references")
        test("MarkdownTest103/Literal quotes in titles")
        test("MarkdownTest103/Nested blockquotes")
        test("MarkdownTest103/Ordered and unordered lists")
        test("MarkdownTest103/Strong and em together")
        test("MarkdownTest103/Tabs")
        test("MarkdownTest103/Tidyness")

        test("MarkdownTest103/Markdown Documentation - Basics")
        test("MarkdownTest103/Markdown Documentation - Syntax")
      }

      "without any enabled extensions" in {
        runMarkdownTestSuite(new PegDownProcessor())
      }

      "with most extensions enabled" in {
        runMarkdownTestSuite {
          new PegDownProcessor(ALL & ~SMARTYPANTS & ~HARDWRAPS & ~ANCHORLINKS)
        }
      }
    }
  }

}
