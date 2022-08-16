TODO 
---
Request
+ Session and APIs - P1 - done
+ Flash - P1 - done
+ Parser - Not required
+ MultiFormData - done
+ Request.locale - done
+ Form - I18N support
+ Implements HttpConfiguration -
+ Need to handle Content-Type better - e.g. Json, Array, InputStream
+ Need to improve the handling of Result/HttpResponse to enable better handling of HttpErrorHandler
+ Deprecated WebServer.addAnnotatedService - use WebServer.addServices
+ Add Armeria HTTP configurations - for use in production

Form (Data)
---
+ Form[A], Field - P1 [Done]
+ Form creation - P1 [Done]
+ validation - P1  [Done]
+ Nested form field - done
+ transform - done
+ implements the rest of Field supported types e.g Temporal, String etc - done
+ implement Field validation error messages - Partial - done, no locale
+ bindFromRequest - binding of numbers can be null (broken) - to verify
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

TODO
+ streaming of files (html, csv), images, 
+ Endpoint url - to implement urlencoded
+ implement App lifecycle

Need to ensure Address is not recyle
[error] 	com.greenfossil.webserver.SessionSuite - need to have assertions


[error] 	com.greenfossil.webserver.HeadersSuite
[error] 	com.greenfossil.webserver.SessionSuite
[error] 	com.greenfossil.webserver.RequestSuite
[error] 	com.greenfossil.data.mapping.QuerystringFormPlayCompatibilitySuite