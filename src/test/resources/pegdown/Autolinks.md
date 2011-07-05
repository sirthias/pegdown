# Autolinks

Autolinks are simple URIs like http://www.parboiled.org,
which will be automatically "activated" by pegdown.

pegdown tries to be smart and not include trailing
punctuation marks like commas and such in the email
and URI links (joe@somewhere.com is such an example).
ftp://somesite.org:1234: this would be another one!

The following links should work just normally:

* [example](http://example)
* [ex@mple](http://example)
* [example://](http://example)