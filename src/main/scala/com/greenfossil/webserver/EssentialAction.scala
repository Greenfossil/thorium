package com.greenfossil.webserver

import com.linecorp.armeria.common.stream.StreamMessage
import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpData, HttpHeaders, HttpRequest, HttpResponse, HttpStatus, MediaType}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.io.{InputStream, PipedInputStream}
import java.time.Duration
import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}

trait Controller

type ActionResponse =  HttpResponse | Result | String | Array[Byte] | InputStream

private[webserver] val _actionLogger = LoggerFactory.getLogger("webserver-action")

trait EssentialAction extends HttpService:

  /**
   * An EssentialAction is an Request => ActionResponse
   * All subclasses must implements this function signature
   *
   * This method will be invoked by EssentialAction.serve
   *
   * @param request
   * @return
   */
  protected def apply(request: Request): ActionResponse

  /**
   * Armeria invocation during an incoming request
   * @param svcRequestContext
   * @param httpRequest
   * @return
   */
  override def serve(svcRequestContext: ServiceRequestContext, httpRequest: HttpRequest): HttpResponse =
    val f: CompletableFuture[HttpResponse] =
      svcRequestContext
        .request()
        .aggregate()
        .thenApplyAsync(aggregateRequest => invokeAction(svcRequestContext, aggregateRequest))
    HttpResponse.from(f)

  private def invokeAction(svcRequestContext: ServiceRequestContext, aggregateRequest: AggregatedHttpRequest): HttpResponse =
    try {
      val req = new com.greenfossil.webserver.Request(svcRequestContext, aggregateRequest) {}
      val resp = apply(req) match
        case s: String => HttpResponse.of(s)
        case hr: HttpResponse => hr
        case result: Result => result.toHttpResponse(req)
        case bytes: Array[Byte] =>
          HttpResponse.of(HttpStatus.OK, Option(req.contentType).getOrElse(MediaType.ANY_TYPE), HttpData.wrap(bytes))
        case is: InputStream =>
          val headers =
            com.linecorp.armeria.common.ResponseHeaders.builder(HttpStatus.OK)
              .contentType(Option(req.contentType).getOrElse(MediaType.ANY_TYPE))
              .build()
          HttpResponse.of(headers, StreamMessage.fromOutputStream(os => {
            var doRead = true
            val READ_BLOCK_SIZE = 8192
            while (doRead) {
              val bytes = is.readNBytes(READ_BLOCK_SIZE)
              doRead = bytes.size == READ_BLOCK_SIZE
              os.write(bytes)
            }
            os.close()
          }))
      resp
    } catch {
      case t: Throwable =>
        _actionLogger.error("Invoke Action error", t)
        HttpResponse.ofFailure(t) // allow exceptionHandlerFunctions and serverErrorHandler to kick in
    }

end EssentialAction

trait Action extends EssentialAction

object Action:

  /**
   * AnyContent request
   * @param actionResponder
   * @return
   */
  def apply(fn: Request => ActionResponse): Action =
    (request: Request) => fn(request)

  /**
   * Multipart form request
   * @param actionResponder
   * @return
   */
  def multipart(fn: MultipartRequest => ActionResponse): Action =
    (request: Request) => request.asMultipartFormData { form =>
      fn(MultipartRequest(form, request.requestContext, request.aggregatedHttpRequest))
    }

  //TODO - need to add test cases
  def async(fn: Request => ActionResponse)(using executor: ExecutionContext): Future[Action] =
    Future((request: Request) => fn(request))
