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

//def Redirect(url: String, status: Int): HttpResponse = Redirect(url, Map.empty, status)

def Redirect(url: String): Result =
  toResult(HttpStatus.SEE_OTHER, url)

def Redirect(url: String, status: HttpStatus): Result =
 toResult(status, url)

//def Redirect(action: Action): Result = ???

def Redirect(url: String, queryString: Map[String, Seq[String]]): Result =
  Redirect(url, queryString, HttpStatus.SEE_OTHER)

def Redirect(url: String, queryString: Map[String, Seq[String]], status: HttpStatus): Result =
  ???

def Redirect(call: Call): Result =  Redirect(call.url)

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

/**
 * Inline redirect macro
 * @param action
 * @return
 */
inline def Redirect(inline action: Action): Result =
  ${ RedirectImpl('action) }

import scala.quoted.*
def RedirectImpl(actionExpr:Expr[Action])(using Quotes): Expr[Result] =
  import quotes.reflect.*

  val (name, annotationTerms, paramNameValueLookup) = actionExpr.asTerm match {
    case Inlined(_, _, methodOwner @ Select(_, name)) =>
      (name, methodOwner.symbol.annotations, Map.empty[String, Any])

    case Inlined(a, b, app @ Apply(Select(Ident(_), name ), args)) =>
      val paramNames: List[String] = app.symbol.paramSymss.head.map(_.name)
      val paramValues: List[Any] = args.collect{case Literal(c) => c.value}
      val paramNameValueLookup: Map[String, Any] = paramNames.zip(paramValues).toMap
      (name, app.symbol.annotations, paramNameValueLookup)

    case Inlined(_,_, app @ Apply( name @ Ident(_), args)) =>
      val paramNames: List[String] = app.symbol.paramSymss.head.map(_.name)
      val paramValues: List[Any] = args.collect{case Literal(c) => c.value}
      val paramNameValueLookup: Map[String, Any] = paramNames.zip(paramValues).toMap
      (name.symbol.name, app.symbol.annotations, paramNameValueLookup)

    case Inlined(_,_, methodOwner @ Ident(name)) =>
      (name, methodOwner.symbol.annotations, Map.empty[String, Any])
  }

  val (method, declaredPath): (String, String) = annotationTerms.collect{
    case Apply(Select(New(x), _), args) =>
      (x.symbol.name, args.collect{case Literal(c) => c}.head.value.toString)
  }.headOption.getOrElse((null, null))

  var usedPathParamNames: List[String] = Nil
  def getPathParam(name: String): Any =
    paramNameValueLookup.get(name) match {
      case Some(value) =>
        usedPathParamNames = usedPathParamNames :+ name
        value
      case None => report.errorAndAbort(s"Path param [${name}] does not match function param name", actionExpr)
    }

  val computedPath =
    if paramNameValueLookup.isEmpty
    then declaredPath
    else {
      val parts = declaredPath.split("/:")
      parts.tail.zipWithIndex.foldLeft(parts.head){(accPath, tup2) =>
        val (part, index) = tup2
        val newPart = part.split("/") match {
          case Array(pathParamName, right) =>
            s"${getPathParam(pathParamName)}/$right"
          case Array(pathParamName) =>
            getPathParam(pathParamName)
        }
        s"$accPath/$newPart"
      }
    }

  val mismatchParams =  paramNameValueLookup.keys.toList diff usedPathParamNames
  if mismatchParams.nonEmpty then report.errorAndAbort("Params mismatch", actionExpr)

  '{Redirect(${Expr(computedPath)})}