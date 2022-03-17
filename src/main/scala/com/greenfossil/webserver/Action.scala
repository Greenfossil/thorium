package com.greenfossil.webserver

import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpHeaders, HttpRequest, HttpResponse, HttpStatus, ResponseHeaders}
import com.linecorp.armeria.server.annotation.{AnnotatedHttpService, AnnotatedHttpServiceSet, RequestConverter, RequestConverterFunction}
import com.linecorp.armeria.server.ServiceRequestContext

import java.lang.reflect.ParameterizedType
import scala.concurrent.{ExecutionContext, Future}

/*
 * https://armeria.dev/docs/server-annotated-service#converting-an-http-request-to-a-java-object
 * https://armeria.dev/docs/server-annotated-service#converting-a-java-object-to-an-http-response
 */
class WebServerRequestConverter extends RequestConverterFunction:
  override def convertRequest(ctx: ServiceRequestContext,
                              request: AggregatedHttpRequest,
                              expectedResultType: Class[_],
                              expectedParameterizedResultType: ParameterizedType): AnyRef =
    val currentThread = Thread.currentThread()
    val oldCl = currentThread.getContextClassLoader
    currentThread.setContextClassLoader(getClass.getClassLoader)
    try {
      if (expectedResultType == classOf[com.greenfossil.webserver.Request])
        new com.greenfossil.webserver.Request(ctx){}
      else
        RequestConverterFunction.fallthrough()
    }finally {
      currentThread.setContextClassLoader(oldCl)
    }

@RequestConverter(classOf[WebServerRequestConverter])
trait Controller extends AnnotatedHttpServiceSet

trait Action(fn: Request => HttpResponse | String) extends AnnotatedHttpService:
  override def serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    import com.linecorp.armeria.scala.implicits._
    given ExecutionContext = ServiceRequestContext.current.eventLoopExecutionContext
    val f = scala.concurrent.Future {
      val req = new Request(ctx) {}
      fn(req) match {
        case s: String => HttpResponse.of(s).withSession(req.session)
        case httpResponse: HttpResponse => httpResponse.withSession(req.session)
      }
    }
    HttpResponse.from(f.toJava)


object Action {

  def apply(fn: Request => HttpResponse | String): Action = new Action(fn){}

  //TODO
  def async(fn: Request => HttpResponse | String): Future[Action] = 
    ???
}


/*
 * Resonse
 */

def Ok[C](body: C)(using w: Writeable[C]): HttpResponse =
  httpResponse(HttpStatus.OK, body)

def BadRequest[C](body: C)(using w: Writeable[C]): HttpResponse =
  httpResponse(HttpStatus.BAD_REQUEST, body)

//def Redirect(url: String, status: Int): HttpResponse = Redirect(url, Map.empty, status)
def Redirect(url: String): HttpResponse = HttpResponse.ofRedirect(HttpStatus.SEE_OTHER, url)

def Redirect(url: String, status: HttpStatus): HttpResponse =
  HttpResponse.ofRedirect(status, url)
  
def Redirect(action: Action): HttpResponse = ???

def Redirect(url: String, queryString: Map[String, Seq[String]]): HttpResponse =
  ???

def Redirect(url: String, queryString: Map[String, Seq[String]], status: HttpStatus): HttpResponse =
  ???

def Redirect(call: Call): HttpResponse =  ???

def NotFound[C](body: C)(using w: Writeable[C]): HttpResponse =
  httpResponse(HttpStatus.NOT_FOUND, body)

def Forbidden[C](body: C)(using w: Writeable[C]): HttpResponse =
  httpResponse(HttpStatus.FORBIDDEN, body)

def InternalServerError[C](body: C)(using w: Writeable[C]): HttpResponse =
  httpResponse(HttpStatus.INTERNAL_SERVER_ERROR, body)

def Unauthorized[C](body: C)(using w: Writeable[C]): HttpResponse = ???

def httpResponse[C](status: HttpStatus, body: C)(using w: Writeable[C]): HttpResponse =
  val (mediaType, bytes) = w.content(body)
  HttpResponse.of(status, mediaType, bytes)
