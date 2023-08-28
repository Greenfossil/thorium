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

import com.greenfossil.commons.json.JsValue
import com.greenfossil.htmltags.Tag
import com.linecorp.armeria.common.{HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.util.concurrent.CompletableFuture

type SimpleResponse = String | Array[Byte] | JsValue | Tag | InputStream | HttpResponse

type ActionResponse = SimpleResponse | Result

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
    actionLogger.debug(s"Processing EssentialAction.serve - method:${svcRequestContext.method()}, content-type:${httpRequest.contentType()}, uri:${svcRequestContext.uri()}")
    val futureResp = new CompletableFuture[HttpResponse]()
    svcRequestContext
      .request()
      .aggregate()
      .thenAccept { aggregateRequest =>
        actionLogger.debug("Setting up blockingTaskExecutor()")
        svcRequestContext.blockingTaskExecutor().execute(() => {
          //Invoke EssentialAction
          var ctxCl = Thread.currentThread().getContextClassLoader
          if ctxCl == null then {
            val ctxCl = this.getClass.getClassLoader
            actionLogger.debug(s"Async setContextClassloader:${ctxCl}")
            Thread.currentThread().setContextClassLoader(ctxCl)
          }
          val httpResp =
            try
              val req = new Request(svcRequestContext, aggregateRequest) {}
              actionLogger.debug(s"Invoke EssentialAction.apply. cl:${ctxCl}, req:${req.hashCode()}")
              val resp = apply(req)
              actionLogger.debug("Response from EssentialAction.apply")
              HttpResponseConverter.convertActionResponseToHttpResponse(req, resp)
            catch
              case t =>
                actionLogger.debug(s"Exception raised in EssentialAction.apply.", t)
                HttpResponse.ofFailure(t)
          futureResp.complete(httpResp)
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
  def apply(fn: Request => ActionResponse): Action =
    actionLogger.debug(s"Processing Action...")
    (request: Request) => fn(request)

  /**
   * Multipart form request
   *
   * @param actionResponder
   * @return
   */
  def multipart(fn: MultipartRequest => ActionResponse): Action =
    actionLogger.debug("Processing Multipart Action...")
    (request: Request) => request.asMultipartFormData { form =>
      fn(MultipartRequest(form, request.requestContext, request.aggregatedHttpRequest))
    }
