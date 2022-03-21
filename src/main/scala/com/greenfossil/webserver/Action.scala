package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{AggregatedHttpRequest, Cookie, CookieBuilder, HttpHeaders, HttpRequest, HttpResponse, HttpStatus, ResponseHeaders}
import com.linecorp.armeria.server.annotation.{AnnotatedHttpService, AnnotatedHttpServiceSet, RequestConverter, RequestConverterFunction}
import com.linecorp.armeria.server.ServiceRequestContext

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

trait Action(fn: Request => Result | String) extends AnnotatedHttpService:
  override def serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    val f: CompletableFuture[HttpResponse] = ctx.request().aggregate().thenApply(aggregateRequest => {
      val req = new Request(ctx, aggregateRequest) {}
      fn(req) match {
        case s: String =>
          createSessionCookie(req.session) match {
            case None => HttpResponse.of(s)
            case Some(cookie) =>
              HttpResponse.of(s).mapHeaders(_.toBuilder.cookies(cookie).build())
          }

        case result:Result =>
          val httpResp = result.body match
            case httpResponse: HttpResponse => httpResponse
            case string: String => HttpResponse.of(string)

          //Forward Session
          val sessionCookieOption: Option[Cookie] = result.newSessionOpt match {
            case None =>
              //Forward Request session
              createSessionCookie(req.session)

            case Some(newSession) if newSession.isEmpty =>
              //Request session will not be forwarded
              None

            case Some(newSession) =>
              //Request session + new session will be forwarded
              val session = req.session + newSession
              createSessionCookie(session)
          }

          //Forward Flash
          val flashCookieOpt: Option[Cookie] = result.newFlashOpt match  //result.newFlashOpt.flatMap(flash => createFlashCookie(flash))
            case None =>
              if req.flash.nonEmpty then Some(Cookie.secureBuilder(RequestAttrs.Flash.name(), "").maxAge(0).build())
              else None
            case Some(flash) =>  createFlashCookie(flash)


          val xs: HttpResponse =  (sessionCookieOption ++ flashCookieOpt).toList match {
            case Nil => httpResp
            case cookies => httpResp.mapHeaders(_.toBuilder.cookies(cookies*).build())
          }

          xs
      }
    })
    HttpResponse.from(f)

private def createSessionCookie(session: Session): Option[Cookie] =
  if session.data.isEmpty then None
  else
    val jwt = Json.toJson(session.data).encodeBase64URL
    println(s"Response Session jwt = ${jwt} ${session.data}")
    Some(Result.bakeCookie(RequestAttrs.Session.name(),jwt))


private def createFlashCookie(flash: Flash): Option[Cookie] =
  if flash.data.isEmpty then None
  else
    val jwt = Json.toJson(flash.data).encodeBase64URL
    println(s"Response Flash jwt = ${jwt} ${flash.data}")
    Some(Result.bakeCookie(RequestAttrs.Flash.name(),jwt))

object Action {

  def apply(fn: Request => Result | String): Action = new Action(fn){}

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

//def Redirect(url: String, status: Int): HttpResponse = Redirect(url, Map.empty, status)

def Redirect(url: String): Result =
  toResult(HttpStatus.SEE_OTHER, url)

def Redirect(url: String, status: HttpStatus): Result =
 toResult(status, url)

def Redirect(action: Action): Result = ???

def Redirect(url: String, queryString: Map[String, Seq[String]]): Result =
  ???

def Redirect(url: String, queryString: Map[String, Seq[String]], status: HttpStatus): Result =
  ???

def Redirect(call: Call): Result =  ???

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
