package experiment.armeria

import com.greenfossil.webserver.{Action, Controller, Request, Result}
import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpHeaders, HttpRequest, HttpResponse, ResponseHeaders}
import com.linecorp.armeria.server.{Server, ServiceRequestContext}
import com.linecorp.armeria.server.annotation.{ExceptionHandlerFunction, Get, RequestConverterFunction, ResponseConverterFunction}

import java.lang.reflect.ParameterizedType
import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}


trait ActionService(val fn: Request => HttpResponse | Result | String)

object ActionService:
  def apply(fn: Request => HttpResponse | Result | String): ActionService =
    new ActionService(fn){}
end ActionService

object ServiceController {
  @Get("/simple")
  def simple = ActionService { request =>
    "HelloWorld!"
  }
}

object ConverterFn extends RequestConverterFunction, ResponseConverterFunction, ExceptionHandlerFunction{
  override def convertRequest(ctx: ServiceRequestContext, request: AggregatedHttpRequest,
                              expectedResultType: Class[_],
                              expectedParameterizedResultType: ParameterizedType): AnyRef =
    println(s"expectedResultType = ${expectedResultType}")
    "Request Converter"

  override def convertResponse(ctx: ServiceRequestContext, headers: ResponseHeaders,
                               result: Any,
                               trailers: HttpHeaders): HttpResponse =
    result match {
      case svc: ActionService =>
        val f: CompletableFuture[HttpResponse] = ctx.request().aggregate().thenApply(aggregateRequest => {
          val req = new Request(ctx, aggregateRequest) {}
          svc.fn(req) match
            case s: String => HttpResponse.of(s)
            case hr: HttpResponse => hr
            case result:Result => result.toHttpResponse(req)
        })
        HttpResponse.from(f)
      case _ => HttpResponse.of(s"Final result + ${result}")
    }

  override def handleException(ctx: ServiceRequestContext, req: HttpRequest, cause: Throwable): HttpResponse =
    HttpResponse.of("An Exception!!!")
}

@main def main = {
  val sb = Server.builder();
  // Configure an HTTP port.
  sb.http(8080);
  sb.annotatedService(ServiceController)
  import scala.jdk.CollectionConverters.*
  sb.annotatedServiceExtensions(List(ConverterFn).asJavaCollection,List(ConverterFn).asJavaCollection, List(ConverterFn).asJavaCollection)
  val server = sb.build();
  val future = server.start();
  future.join();
}



