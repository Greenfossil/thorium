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
 * The return type of an action block. An action may return either:
 *   - a synchronous `ActionResponse` (String, Result, JsValue via Conversion, etc.)
 *   - a `Future[ActionResponse]` for async operations
 *
 * The framework's `serve()` unifies both via `Future(apply(req)).flatMap`:
 *   - Sync returns are wrapped in `Future.successful(ar)` — zero scheduling overhead.
 *   - Async returns (Future) are chained directly — the blocking-pool thread is freed
 *     immediately after construction, and completion fires on the Future's own thread.
 *
 * This means `Action.async { req => "hello" }` works (sync return in an async factory)
 * and `Action { req => Future(Ok("hello")) }` also works (async return in a sync factory).
 * `Action.async` is a semantic marker — the pipeline handles both identically.
 */
type AsyncActionResponse = ActionResponse | Future[ActionResponse]

private[thorium] val actionLogger = LoggerFactory.getLogger("com.greenfossil.thorium.action")

trait EssentialAction extends HttpService :

  /**
   * An EssentialAction is a `Request => AsyncActionResponse`.
   * All subclasses must implement this function signature.
   *
   * This method will be invoked by [[EssentialAction.serve]].
   *
   * @param request
   * @return
   */
  protected def apply(request: Request): AsyncActionResponse

  /**
   * Armeria invocation during an incoming request.
   *
   * Unified pipeline:
   *   1. Aggregate the request body (on the event loop).
   *   2. `Future(apply(req))` — run the action body on the blocking-pool executor.
   *      For sync actions, the blocking pool holds for the full duration (may block on JDBC/HTTP).
   *      For async actions, the blocking pool holds only for Future construction (microseconds).
   *   3. `.flatMap` — sync `ActionResponse` → `Future.successful(ar)` (already completed, no overhead);
   *      `Future[ActionResponse]` → chain the inner Future directly (blocking pool thread freed).
   *   4. `.onComplete` (with `ExecutionContext.parasitic`) — fires on whatever thread completes
   *      the Future. Converts the `ActionResponse` to an `HttpResponse` and completes the
   *      Armeria `CompletableFuture`. `futureResp.complete(...)` is thread-safe.
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
        val req = new Request(svcRequestContext, aggregateRequest) {}
        given ec: ExecutionContext = ExecutionContext.fromExecutorService(svcRequestContext.blockingTaskExecutor())

        // Unified pipeline: both sync and async go through the same path.
        // Future(apply(req)) runs apply(req) on the blocking pool.
        // .flatMap: sync → Future.successful (already completed); async → chain inner Future.
        // .onComplete (parasitic): convert + complete futureResp on the completing thread.
        Future(apply(req))
          .flatMap {
            case f: Future[ActionResponse] @unchecked => f
            case ar: ActionResponse => Future.successful(ar)
          }
          .onComplete {
            case Success(ar) =>
              futureResp.complete(
                HttpResponseConverter.convertActionResponseToHttpResponse(req, ar)
              )
            case Failure(ex) =>
              actionLogger.debug(s"Action failed.", ex)
              futureResp.complete(HttpResponse.ofFailure(ex))
          }(using ExecutionContext.parasitic)
      }
    HttpResponse.of(futureResp)

end EssentialAction

trait Action extends EssentialAction

object Action:

  /**
   * Creates a synchronous action. The action body may return any `AsyncActionResponse`
   * (including `Future[ActionResponse]` — the pipeline handles both).
   *
   * @param fn the action body
   * @return an [[Action]]
   */
  def apply(fn: Request => AsyncActionResponse): Action =
    actionLogger.debug(s"Processing Action...")
    (request: Request) => fn(request)

  /**
   * Multipart form request. The action body receives a [[MultipartRequest]]
   * and may return any `AsyncActionResponse`.
   *
   * @param fn the action body
   * @return an [[Action]]
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
   * Creates an async action. The action body may return:
   *   - A `Future[ActionResponse]` for genuinely async work (DB calls, HTTP requests, etc.)
   *   - A synchronous `ActionResponse` (String, Result, Option, Try, Either, JsValue, etc.)
   *     — the framework wraps it in `Future.successful` internally.
   *
   * This is a semantic marker — the pipeline handles both sync and async returns
   * identically. The user does not need to wrap sync returns in `Future(...)`.
   *
   * The user's `Future` body runs on the user's own `ExecutionContext` (typically
   * `given ExecutionContext = ExecutionContext.global` declared at the controller level).
   * The framework does not require or capture an `ExecutionContext` — completion
   * is wired via `ExecutionContext.parasitic` inside `serve()`.
   *
   * Example:
   * {{{
   * given ExecutionContext = ExecutionContext.global
   *
   * @Get("/search")
   * def search: Action = Action.async { req =>
   *   db.findUser(req.queryParam("q")).map(Ok(_))   // Future[Result]
   * }
   *
   * @Get("/sync-in-async")
   * def syncInAsync: Action = Action.async { req =>
   *   "hello"   // String — wrapped in Future.successful internally
   * }
   * }}}
   *
   * @param fn the action body returning `AsyncActionResponse`
   * @return an [[Action]]
   */
  def async(fn: Request => AsyncActionResponse): Action =
    actionLogger.debug(s"Processing Async Action...")
    (request: Request) => fn(request)

  /**
   * Multipart form request with async support. Same semantics as [[async]] —
   * the action body may return sync or async values.
   *
   * @param fn the action body returning `AsyncActionResponse`
   * @return an [[Action]]
   */
  def multipartAsync(fn: MultipartRequest => AsyncActionResponse): Action =
    actionLogger.debug("Processing Multipart Async Action...")
    (request: Request) => request.asMultipartFormData { form =>
      fn(MultipartRequest(form, request.requestContext, request.aggregatedHttpRequest))
    }