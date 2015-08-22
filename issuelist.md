List of issues addressed or tested with new version which should be closed

#128, #129, #131, #144, #175, #176, #177, #178, #179, #180

Additionally, made changes to the default ToHtmlSerializer and Printer to output prettier HTML for dd, dt, li, p tags to break up very long lines.

Added testAlt and testASTAlt to AbstractPegDownSpec so that expectations for tests that test's expectation file can have a suffix added. Used for running multiple parser configurations on the same source but having different expected results. 

Hello World
---
***
---
___

Code example
```ruby
a = b
```
