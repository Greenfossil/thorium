/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium

import com.linecorp.armeria.common.{HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.time.Duration
import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

type SimpleResponse = String | Array[Byte] | InputStream | HttpResponse

type ActionResponse = SimpleResponse | Result

/**
 * The async variant of [[ActionResponse]]. An action block may return either
 * a synchronous `ActionResponse` or a `Future[ActionResponse]`. The framework
 * detects which and handles it accordingly — sync responses are converted
 * directly; futures are wired to the Armeria response pipeline without blocking.
 *
 * Note: returning a `Future` from an action block requires an `ExecutionContext`.
 * Use [[Action.async]] which captures the given `ExecutionContext` at call site.
 */
type AsyncActionResponse = ActionResponse | Future[ActionResponse]

private[thorium] val actionLogger = LoggerFactory.getLogger("com.greenfossil.thorium.action")

trait EssentialAction extends HttpService :

  /**
   * An EssentialAction is an Request => ActionResponse
   * All subclasses must implements this function signature
   *
   * This method will be invoked by EssentialAction.serve
   *
   * @param request
   * @return
   */
  protected def apply(request: Request): AsyncActionResponse

  /**
   * Armeria invocation during an incoming request
   *
   * @param svcRequestContext
   * @param httpRequest
   * @return
   */
  override def serve(svcRequestContext: ServiceRequestContext, httpRequest: HttpRequest): HttpResponse =
    actionLogger.debug(s"Processing EssentialAction.serve - method:${svcRequestContext.method()}, content-type:${httpRequest.contentType()}, path:${httpRequest.uri}")
    val futureResp = new CompletableFuture[HttpResponse]()
    svcRequestContext
      .request()
      .aggregate()
      .thenAccept { aggregateRequest =>
        actionLogger.debug("Setting up blockingTaskExecutor()")
        svcRequestContext.blockingTaskExecutor().execute(() => {
          val ctxCl = Thread.currentThread().getContextClassLoader
          if ctxCl == null then {
            val cl = this.getClass.getClassLoader
            actionLogger.debug(s"Async setContextClassloader:${cl}")
            Thread.currentThread().setContextClassLoader(cl)
          }
          try
            val req = new Request(svcRequestContext, aggregateRequest) {}
            actionLogger.debug(s"Invoke EssentialAction.apply. cl:$ctxCl, req:${req.hashCode()}")
            val resp = apply(req)
            actionLogger.debug("Response from EssentialAction.apply")
            resp match
              case future: Future[ActionResponse] @unchecked =>
                // Async path: wire Future completion to the response CompletableFuture
                // without blocking the blockingTaskExecutor thread.
                future.onComplete {
                  case Success(actionResp) =>
                    val httpResp = HttpResponseConverter.convertActionResponseToHttpResponse(req, actionResp)
                    futureResp.complete(httpResp)
                  case Failure(ex) =>
                    actionLogger.debug(s"Async action Future failed.", ex)
                    futureResp.complete(HttpResponse.ofFailure(ex))
                }(using ExecutionContext.parasitic)
              case actionResp: ActionResponse =>
                // Sync path: convert and complete immediately
                val httpResp = HttpResponseConverter.convertActionResponseToHttpResponse(req, actionResp)
                futureResp.complete(httpResp)
          catch
            case t =>
              actionLogger.debug(s"Exception raised in EssentialAction.apply.", t)
              futureResp.complete(HttpResponse.ofFailure(t))
        })
      }
    HttpResponse.of(futureResp)

end EssentialAction

trait Action extends EssentialAction

object Action:

  /**
   * AnyContent request
   *
   * @param actionResponder
   * @return
   */
  def apply(fn: Request => AsyncActionResponse): Action =
    actionLogger.debug(s"Processing Action...")
    (request: Request) => fn(request)

  /**
   * Multipart form request
   *
   * @param actionResponder
   * @return
   */
  def multipart(fn: MultipartRequest => AsyncActionResponse): Action =
    actionLogger.debug("Processing Multipart Action...")
    (request: Request) => request.asMultipartFormData { form =>
      fn(MultipartRequest(form, request.requestContext, request.aggregatedHttpRequest))
    }

  /**
   * Clears the request timeout for this action, allowing long-running
   * operations (e.g. LLM streaming, bulk DB operations) to complete without
   * Armeria's default request timeout interrupting.
   *
   * Use sparingly — only for endpoints that genuinely need unlimited time.
   * Prefer [[Action.timeout]] with an explicit duration where possible.
   *
   * @param fn the action body
   * @return an [[Action]] with no request timeout
   */
  def noTimeout(fn: Request => AsyncActionResponse): Action =
    (request: Request) =>
      request.requestContext.clearRequestTimeout()
      fn(request)

  /**
   * Sets an explicit request timeout for this action, overriding the server's
   * default. Useful for endpoints that need more (or less) time than the
   * global default.
   *
   * @param duration the maximum time the request is allowed to run
   * @param fn the action body
   * @return an [[Action]] with the specified timeout
   */
  def timeout(duration: Duration)(fn: Request => AsyncActionResponse): Action =
    (request: Request) =>
      request.requestContext.setRequestTimeout(duration)
      fn(request)

  /**
   * Creates an async action that returns a `Future[ActionResponse]`.
   *
   * The action body runs on the `blockingTaskExecutor` as usual, but instead
   * of returning a synchronous `ActionResponse`, it returns a `Future`. The
   * framework wires the `Future`'s completion to the Armeria response pipeline
   * without blocking the executor thread.
   *
   * This eliminates the need for `Await.result` in controllers that perform
   * async operations (DB calls, HTTP requests, LLM streaming, etc.).
   *
   * The `ExecutionContext` is captured from the implicit scope at the call site.
   * Typically `given ExecutionContext = ExecutionContext.global` is declared at
   * the controller object level.
   *
   * Example:
   * {{{
   * given ExecutionContext = ExecutionContext.global
   *
   * @Get("/search")
   * def search: Action = Action.async { req =>
   *   db.findUser(req.queryParam("q")).map(Ok(_))   // Future[Result]
   * }
   * }}}
   *
   * @param fn the action body returning a `Future[ActionResponse]`
   * @param ec the ExecutionContext for the Future (implicit)
   * @return an [[Action]] whose response is async
   */
  def async(fn: Request => Future[ActionResponse])(using ExecutionContext): Action =
    actionLogger.debug(s"Processing Async Action...")
    (request: Request) => fn(request)

  /**
   * Multipart form request with async response.
   *
   * @param fn the action body returning a `Future[ActionResponse]`
   * @param ec the ExecutionContext for the Future (implicit)
   * @return an [[Action]] whose response is async
   */
  def multipartAsync(fn: MultipartRequest => Future[ActionResponse])(using ExecutionContext): Action =
    actionLogger.debug("Processing Multipart Async Action...")
    (request: Request) => request.asMultipartFormData { form =>
      fn(MultipartRequest(form, request.requestContext, request.aggregatedHttpRequest))
    }
