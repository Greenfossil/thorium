package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Request as _, *}
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.{AnnotatedHttpService, AnnotatedHttpServiceSet, RequestConverter, RequestConverterFunction}

import java.lang.reflect.ParameterizedType
import java.util.concurrent.CompletableFuture
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
        new com.greenfossil.webserver.Request(ctx, request){}
      else
        RequestConverterFunction.fallthrough()
    }finally {
      currentThread.setContextClassLoader(oldCl)
    }

@RequestConverter(classOf[WebServerRequestConverter])
trait Controller extends AnnotatedHttpServiceSet

trait Action(fn: Request => HttpResponse | Result | String) extends AnnotatedHttpService:
  override def serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    val f: CompletableFuture[HttpResponse] = ctx.request().aggregate().thenApply(aggregateRequest => {
      //Set Class Loader from current thread for Config AppSettings used by DB
      Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader)

      val req = new Request(ctx, aggregateRequest) {}
      fn(req) match {
        case s: String => HttpResponse.of(s)
        case hr: HttpResponse => hr
        case result:Result => result.toHttpResponse(req)
      }
    })
    HttpResponse.from(f)


object Action {

  def apply(fn: Request => HttpResponse | Result | String): Action = new Action(fn){}

  //TODO
  def async(fn: Request => Result): Future[Action] =
    ???
}


/*
 * Resonse
 */

def Ok[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.OK, body)

def BadRequest[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.BAD_REQUEST, body)

def Redirect(url: String): Result =
  toResult(HttpStatus.SEE_OTHER, url)

def Redirect(url: String, status: HttpStatus): Result =
 toResult(status, url)

def Redirect(url: String, queryString: Map[String, Seq[String]]): Result =
  Redirect(url, queryString, HttpStatus.SEE_OTHER)

def Redirect(url: String, queryString: Map[String, Seq[String]], status: HttpStatus): Result =
  ???

@deprecated("to remove")
def Redirect(call: Call): Result =  Redirect(call.url)

/**
 * Inline redirect macro
 * @param action
 * @return
 */
inline def Redirect(inline action: Action): Result = RedirectMcr(action)

def NotFound[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.NOT_FOUND, body)

def Forbidden[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.FORBIDDEN, body)

def InternalServerError[C](body: C)(using w: Writeable[C]): Result =
  toResult(HttpStatus.INTERNAL_SERVER_ERROR, body)

def Unauthorized[C](body: C)(using w: Writeable[C]): Result = ???

private def toResult[C](status: HttpStatus, body: C)(using w: Writeable[C]): Result =
  if /*Redirect*/ status.code() >= 300 && status.code() <= 308
  then
    Result(HttpResponse.ofRedirect(status, body.asInstanceOf[String]))
  else
    val (mediaType, bytes) = w.content(body)
    Result(HttpResponse.of(status, mediaType, bytes))