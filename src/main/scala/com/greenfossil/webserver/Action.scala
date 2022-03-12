package com.greenfossil.webserver

import com.linecorp.armeria.common.{HttpRequest, HttpResponse, HttpStatus}
import com.linecorp.armeria.server.annotation.{AnnotatedHttpServiceSet, AnnotatedHttpService}
import com.linecorp.armeria.server.ServiceRequestContext

trait Controller extends AnnotatedHttpServiceSet

trait Action(fn: Request => HttpResponse) extends AnnotatedHttpService:
  override def serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    val req = new Request(ctx) {}
    HttpResponse.from(() => fn(req), ctx.blockingTaskExecutor())


object Action {

  def apply(fn: Request => HttpResponse): Action = new Action(fn){}

  //TODO
  def async = ???
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
