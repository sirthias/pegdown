Introduction
------------

_pegdown_ is a pure Java library for clean and lightweight [Markdown][] processing.
It's implementation is based on a [parboiled][] PEG parser and is therefore rather easy to understand and extend.

_pegdown_ is 100% compatible with the original Markdown specification and fully passes the original Markdown test suite. 
On top of the standard Markdown feature set _pegdown_ will implement a number of extensions similar to what [PHP Markdown Extra][] offers. Currently _pegdown_ offers only two small extensions over standard Markdown:

* SMARTS: Beautifys apostrophes, dashes and ellipsises.
* QUOTES: Beautifys single and double quotes.

More extensions like support for tables, etc. are planned.

Installation
------------

Download the JAR for latest version from the [download page][].  
Additionally _pegdown_ requires the [parboiled][] JAR on the classpath (v0.9.7.0 or better).

Documentation
-------------

Using _pegdown_ is very simple: Just create a new instance of a [PegDownProcessor][] and call its markdownToHtml(String) method to convert the given Markdown source to an HTML string.

The first time you create a [PegDownProcessor][] it can take up to a few hundred milliseconds to prepare the underlying parboiled parser instance, however, once the first processor has been built all further instantiations will be fast. Also, you can reuse an existing [PegDownProcessor][] instance as often as you want, as long as you prevent concurrent accesses, since neither the [PegDownProcessor][] nor the underlying parser is thread-safe.

See <http://sirthias.github.com/pegdown/api> for the pegdown API documation.

Credits
-------

The underlying PEG grammar was developed by John MacFarlane and made available with his
tool [peg-markdown](http://github.com/jgm/peg-markdown).   


Any feedback is, of course, very welcome.
  
   [Markdown]: http://daringfireball.net/projects/markdown/ "Main Markdown site"
   [parboiled]: http://www.parboiled.org
   [PHP Markdown Extra]: http://michelf.com/projects/php-markdown/extra/#html
   [Download Page]: http://github.com/sirthias/pegdown/downloads
   [PegDownProcessor]: http://sirthias.github.com/pegdown/api/org/pegdown/PegDownProcessor.html