CamelXtokenizer
===============

Show Camels xtokenizer() splitter and it's dependency to a valid StAX implementation.

This sample is the result of some experiments on the Camel user list.
It demonstrates the use of the xtokenizer() splitter method (new in Camel 2.14.0) and
its dependency on a correct StAX Location API implementation.

Having a wrong implementation could result in invalid xml after the split. This sample 
shows that.

For easier use you could start it directly via
  ```mvn test```
which generates the error. A start with
  ```mvn test -Pwoodstox```
will use the Woodstox StAX implementation, which is a valid one. So here the test pass.     
 