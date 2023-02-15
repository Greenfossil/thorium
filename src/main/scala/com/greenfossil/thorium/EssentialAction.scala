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

import com.linecorp.armeria.common.{Request as _, *}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.util.concurrent.CompletableFuture
import scala.util.Try

type ActionResponse = HttpResponse | Result | String | Array[Byte] | InputStream

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
  protected def apply(request: Request): ActionResponse

  /**
   * Armeria invocation during an incoming request
   *
   * @param svcRequestContext
   * @param httpRequest
   * @return
   */
  override def serve(svcRequestContext: ServiceRequestContext, httpRequest: HttpRequest): HttpResponse =
    val f = new CompletableFuture[HttpResponse]()
    svcRequestContext
      .request()
      .aggregate()
      .thenApply { aggregateRequest =>
        svcRequestContext.blockingTaskExecutor().execute(() => {
          //Invoke EssentialAction
          val ctxCl = Thread.currentThread().getContextClassLoader
          actionLogger.trace(s"Async thread:${Thread.currentThread()}, asyncCl:${ctxCl}")
          if ctxCl == null then {
            val cl = this.getClass.getClassLoader
            actionLogger.trace(s"Async setContextClassloader:${cl}")
            Thread.currentThread().setContextClassLoader(cl)
          }
          val req = new Request(svcRequestContext, aggregateRequest) {}
          Try(apply(req)).fold(
            t =>
              f.complete(HttpResponse.ofFailure(t)),
            resp =>
              f.complete(HttpResponseConverter.convertActionResponseToHttpResponse(req,resp))
          )
          Thread.currentThread().setContextClassLoader(ctxCl)
        })
      }
    HttpResponse.from(f)

end EssentialAction

trait Action extends EssentialAction

object Action:

  /**
   * AnyContent request
   *
   * @param actionResponder
   * @return
   */
  def apply(fn: Request => ActionResponse): Action =
    (request: Request) => fn(request)

  /**
   * Multipart form request
   *
   * @param actionResponder
   * @return
   */
  def multipart(fn: MultipartRequest => ActionResponse): Action =
    (request: Request) => request.asMultipartFormData { form =>
      fn(MultipartRequest(form, request.requestContext, request.aggregatedHttpRequest))
    }
