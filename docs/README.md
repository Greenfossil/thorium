TODO 
---
Request
+ Session and APIs - P1 - done
+ Flash - P1 - done
+ Parser - Not required
+ MultiFormData 
+ Request.locale - done
+ Form - I18N support
+ Implements HttpConfiguration -

Form (Data)
---
+ Form[A], Field - P1 [Done]
+ Form creation - P1 [Done]
+ validation - P1  [Done]
+ Nested form field - done
+ transform - done
+ implements the rest of Field supported types e.g Temporal, String etc
+ implement Field validation error messages - Partial
+ bindFromRequest - binding of numbers can be null (broken)
HttpErrorHandler - done
---
+ P1 [DONE - HttpErrorHandler]
+ 
Routes
---
+ Route declaration - P1 - done
+ Call / Endpoint - P1 - done


Bootstrap
---
+ Main class

Configuration
---
+ With and without config file ie., either declarative or programmatic


Headers
---
+ Removed, now is part of Request

Caused by: munit.ComparisonFailException: /home/chungonn/development/scala3/web-server/src/test/scala/com/greenfossil/webserver/FormBindingSuite.scala:18
17:      println(s"aggResp.contentUtf8() = ${aggResp.contentUtf8()}")
18:      assertEquals(aggResp.status(), HttpStatus.BAD_REQUEST) // FIXME binding of the longnumber can be null