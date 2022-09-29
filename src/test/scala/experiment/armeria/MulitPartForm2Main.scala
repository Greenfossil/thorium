package experiment.armeria

import com.linecorp.armeria.common.multipart.{Multipart, MultipartFile}
import com.linecorp.armeria.common.*
import com.linecorp.armeria.server.*
import com.linecorp.armeria.server.annotation.*

import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture


@main def multipartFormMain =
  val sb = Server.builder();
  sb.http(8080)

  //curl -v  -F person=homer http://localhost:8080/mp-form-agg\?queryArg\=test
  sb.service("/mp-form-agg", (ctx, req) => {
    val f = new CompletableFuture[HttpResponse]()
    ctx.request().aggregate().thenCompose { aggReq =>
      val queryArg = ctx.queryParam("queryArg")
      val person = Multipart.from(aggReq.toHttpRequest).aggregate().thenApply(aggMp => aggMp.field("person").contentAscii()).join()
      val _resp = HttpResponse.of(s"queryArg:[${queryArg}] person:[${person}]")
      f.complete(_resp)
      f
    }
    HttpResponse.from(f)
  })

  //curl -v  -F person=homer http://localhost:8080/mp-form\?queryArg\=test
  sb.annotatedService(new Object() {
    @Post("/mp-form")
    def upload(@Param person: String, @Param queryArg: String) = {
      HttpResponse.of( s"queryArg:[${queryArg}] person:[${person}]")
    }
  })

  sb.decorator((delegate, ctx, req) => ???)

  object MultiPartAction {
    def apply(fn: AggregatedHttpRequest => HttpResponse): MultiPartAction =
      (req: AggregatedHttpRequest) => fn(req)
  }

  trait MultiPartAction {

    def apply(aggRequest: AggregatedHttpRequest): HttpResponse

    def serve(ctx: ServiceRequestContext) =
      val f1 = ctx.request().aggregate().thenCompose { aggReg =>
        val f =  new CompletableFuture[HttpResponse]()
        ctx.blockingTaskExecutor().execute(() => {
          f.complete(apply(aggReg))
        })
        f
      }
      HttpResponse.from(f1)
  }

  sb.annotatedService(new Object {
    @Post("/mp-action") //curl -v  -F person=homer http://localhost:8080/mp-action\?queryArg\=test
    def form(@Param person: String, @Param queryArg: String) = {
      MultiPartAction { aggReq =>
        HttpResponse.of(s"queryArg:[${queryArg}] person:[${person}]")
      }
    }
  })

  val actionResponseConverter: ResponseConverterFunction =
    (svcRequestContext: ServiceRequestContext, headers: ResponseHeaders, result: Any, trailers: HttpHeaders) =>
      result match
        case action: MultiPartAction => action.serve(svcRequestContext)
        case _ => ResponseConverterFunction.fallthrough()

  import scala.jdk.CollectionConverters.*
  sb.annotatedServiceExtensions(
    List.empty[RequestConverterFunction].asJava,
    List(actionResponseConverter).asJava,
    List.empty[ExceptionHandlerFunction].asJava)

  val server = sb.build()
  val future = server.start()
  future.join()
