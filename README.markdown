Introduction
------------

_pegdown_ is a pure Java library for clean and lightweight [Markdown][] processing.  
It's implementation is based on a [parboiled][] PEG parser and is therefore rather easy to understand and extend.

_pegdown_ is nearly 100% compatible with the original Markdown specification and fully passes the original Markdown test suite.
On top of the standard Markdown feature set _pegdown_ implements a number of extensions similar to what other popular Markdown processors offer.  
Currently _pegdown_ supports the following extensions over standard Markdown:

* SMARTS: Beautifys apostrophes, ellipsises ("..." and ". . .") and dashes ("--" and "---")
* QUOTES: Beautifys single quotes, double quotes and double angle quotes (&laquo; and &raquo;)
* SMARTYPANTS: Convenience extension enabling both, SMARTS and QUOTES, at once.
* ABBREVIATIONS: Support for abbreviations in the way of [PHP Markdown Extra][].
* HARDWRAPS: Enables alternative handling of newlines, see [Github-flavoured-Markdown][]
* AUTOLINKS: Enables plain (undelimited) autolinks the way [Github-flavoured-Markdown][] implements them.
* TABLES: Enables support for tables similar to [MultiMarkdown][] (which is in turn like the [PHP Markdown Extra][] tables, but with colspan support).
* HTML BLOCK SUPPRESSION: Suppresses the output of HTML blocks.
* INLINE HTML SUPPRESSION: Suppresses the output of inline HTML elements.

Note: _pegdown_ differs from the original Markdown in that it ignores in-word emphasis as in

    > my_file.txt
    > 2*3*4=5

Currently this "extension" cannot be switched off.


Installation
------------

You have two options:

* Download the JAR for the latest version from the [download page][].
  _pegdown_ 0.9.2 has a dependency on [parboiled for Java][parboiled], version 0.11.1.
   
* Point your Maven-compatible build tool to the repository at <http://scala-tools.org/repo-releases>,
  group id "**org.pegdown**", artifact "**pegdown**".    
  There may also be snapshot builds available at <http://scala-tools.org/repo-snapshots>. 


Usage
-----

Using _pegdown_ is very simple: Just create a new instance of a [PegDownProcessor][] and call one of its
_markdownToHtml_ methods to convert the given Markdown source to an HTML string.

You can also use pegdown only for the actual parsing of the Markdown source and do the serialization to the
target format (e.g. XML) yourself. To do this just call the _parseMarkdown_ method of the [PegDownProcessor][] to obtain
the root node of the Astract Syntax Tree for the document.
With a custom [Visitor][] implementation you can do whatever serialization you want. As an example you might want to
take a look at the [sources of the ToHtmlSerializer](https://github.com/sirthias/pegdown/blob/develop/src/main/java/org/pegdown/ToHtmlSerializer.java).

Note that the first time you create a [PegDownProcessor][] it can take up to a few hundred milliseconds to prepare the
underlying parboiled parser instance. However, once the first processor has been built all further instantiations will
be fast. Also, you can reuse an existing [PegDownProcessor][] instance as often as you want, as long as you prevent
concurrent accesses, since neither the [PegDownProcessor][] nor the underlying parser is thread-safe.

See <http://sirthias.github.com/pegdown/api> for the pegdown API documation.


Hacking on pegdown
------------------

pegdown uses [Apache Buildr](http://buildr.apache.org) for managing the build process. However, if you do not want to
install Buildr on your machine, it should be no problem to quickly set up a project structure for the IDE of your choice
around the sources, since they are quite compact and the dependencies are few.

In order to provide custom markdown extensions to pegdown you shouldn't even have to get your hands dirty with its
sources. A [PegDownProcessor][] can be constructed around a given Parser instance, so you can supply your own.
If you want to go that way probably want to subclass the _org.pegdown.Parser_ class and override a limited
number of rules to inject your own extensions. Your extensions can create custom [Node](http://www.decodified.com/pegdown/api/org/pegdown/ast/Node.html)
implementations, which you can process through the general _visit(Node)_ method of a custom [Visitor][] implementation.


Credits
-------

A large part of the underlying PEG grammar was developed by John MacFarlane and made available with his
tool [peg-markdown](http://github.com/jgm/peg-markdown).   


License
-------

_pegdown_ is licensed under [APL 2.0](http://www.apache.org/licenses/LICENSE-2.0).


Patch Policy
------------

Feedback and contributions to the project, no matter what kind, are always very welcome.
However, patches can only be accepted from their original author.
Along with any patches, please state that the patch is your original work and that you license the work to the pegdown project under the project’s open source license.
  
   [Markdown]: http://daringfireball.net/projects/markdown/ "Main Markdown site"
   [parboiled]: http://www.parboiled.org
   [PHP Markdown Extra]: http://michelf.com/projects/php-markdown/extra/#html
   [Download Page]: http://github.com/sirthias/pegdown/downloads
   [PegDownProcessor]: http://sirthias.github.com/pegdown/api/org/pegdown/PegDownProcessor.html
   [Github-flavoured-Markdown]: http://github.github.com/github-flavored-markdown/
   [MultiMarkdown]: http://fletcherpenney.net/multimarkdown/users_guide/multimarkdown_syntax_guide/
   [Visitor]: http://www.decodified.com/pegdown/api/org/pegdown/ast/Visitor.html