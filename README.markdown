pegdown
=======

Introduction
------------

_pegdown_ is a pure Java library for clean and lightweight [Markdown][] processing.
It's implementation is based on a [parboiled][] PEG parser and is therefore rather easy to understand and extend.

_pegdown_ is 100% compatible with the original Markdown specification and fully passes the original Markdown test suite. 
On top of the standard Markdown feature set _pegdown_ will implement a number of extensions similar to what [PHP Markdown Extra] offers. Currently _pegdown_ offers only two small extensions over standard Markdown:

* SMARTS: Beautifys apostrophes, dashes and ellipsises.
* QUOTES: Beautifys single and double quotes.

More extensions like support for tables, etc. are planned.

Installation
------------

Download the JAR for latest version from the [Download Page].  
Additionally to its own JAR _pegdown_ requires the main [parboiled][] JAR on the classpath (v0.9.7.0 or better).

Documentation
-------------

Using _pegdown_ is very simple: Just create a new instance of a [PegDownProcessor][] and call its markdownToHtml(String) method to convert the given Markdown source to an HTML string.

The first time you call create a [PegDownProcessor][] it will take some 

See <http://sirthias.github.com/parboiled/api> for the pegdown API documation.



Any feedback is, of course, very welcome.
  
   [Markdown]: http://daringfireball.net/projects/markdown/ "Main Markdown site"
   [parboiled]: http://www.parboiled.org
   [PHP Markdown Extra]: http://michelf.com/projects/php-markdown/extra/#html