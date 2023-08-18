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

import com.linecorp.armeria.common.{Cookie, HttpData, HttpHeaderNames, HttpMethod, HttpResponse, HttpStatus, MediaType, ResponseHeaders}
import com.linecorp.armeria.common.stream.StreamMessage

import java.io.InputStream
import scala.util.{Try, Using}

object HttpResponseConverter:

  private def addCookiesToHttpResponse(cookies: Seq[Cookie], resp: HttpResponse): HttpResponse =
    if cookies.isEmpty then resp
    else resp.mapHeaders(_.toBuilder.cookies(cookies *).build())

  private def addHeadersToHttpResponse(responseHeader: ResponseHeader, resp: HttpResponse): HttpResponse =
    if responseHeader == null || responseHeader.headers.isEmpty then resp
    else resp.mapHeaders(_.withMutations { builder =>
      responseHeader.headers.map { header =>
        builder.set(header._1, header._2)
      }
    })

  private def addContentTypeToHttpResponse(contextTypeOpt: Option[MediaType], resp: HttpResponse): HttpResponse =
    contextTypeOpt match
      case Some(contentType) =>
        resp.mapHeaders(_.withMutations { builder =>
          builder.contentType(contentType)
        })
      case None => resp

  private def getCSRFCookie(req: Request): Option[Cookie] =
    val token = req.requestContext.attr(RequestAttrs.CSRFToken)
    if token == null then None
    else
      val config = req.requestContext.attr(RequestAttrs.Config)
      serverLogger.debug(s"CSRFToken added to header:${token}")
      Option(CookieUtil
        .csrfCookieBuilder(config.httpConfiguration.csrfConfig, token)
        .build())


  private def getAllCookies(req: Request, actionResp: ActionResponse): Seq[Cookie] =
    val (newCookies, newSessionOpt, newFlashOpt) =
      actionResp match
        case result: Result => (result.newCookies, result.newSessionOpt, result.newFlashOpt)
        case _ => (Nil, None, None)
    (getNewSessionCookie(req, newSessionOpt) ++ getNewFlashCookie(req, newFlashOpt) ++  getCSRFCookie(req)).toList ++ newCookies

  private def responseHeader(req: Request, actionResp: ActionResponse): ResponseHeader =
    actionResp match
      case result: Result => result.header
      case _ => null

  private def contentTypeOpt(req: Request, actionResp: ActionResponse): Option[MediaType] =
    actionResp match
      case result: Result => result.contentTypeOpt
      case _ => None

  /*
   * Keep current request Session, if not new values
   * Discard session is newSession isEmpty or append new values
   */
  private def getNewSessionCookie(req: Request, newSessionOpt: Option[Session]): Option[Cookie] =
    newSessionOpt.map { newSession =>
        //If newSession isEmtpy, expire session cookie
        if newSession.isEmpty then
          CookieUtil.bakeDiscardCookie(req.httpConfiguration.sessionConfig.cookieName)(using req)
        else
          //Append new session will to session cookie
          val session = req.session + newSession
          CookieUtil.bakeSessionCookie(session)(using req).orNull
      }

  private def getNewFlashCookie(req: Request, newFlashOpt: Option[Flash]): Option[Cookie] =
    /*
     * Flash cookie could survive for 1 request.
     * However, if there is X-THORIUM-FLASH:Again header, flash will survive 1 more request
     */
    val hasThoriumFlashHeader =
      req.getHeader("X-THORIUM-FLASH").exists("AGAIN".equalsIgnoreCase)
        && HttpMethod.GET == req.method
    val flashOpt = if !hasThoriumFlashHeader then newFlashOpt else newFlashOpt.map(newFlash => req.flash ++ newFlash ).orElse(Option(req.flash))
    flashOpt.flatMap { newFlash =>
      CookieUtil.bakeFlashCookie(newFlash)(using req) //Create a new flash cookie
    }.orElse {
      //Expire the current flash cookie
      if req.flash.isEmpty then None
      else Some(CookieUtil.bakeDiscardCookie(req.httpConfiguration.flashConfig.cookieName)(using req))
    }

  private def _addHttpResponseHeaders(req: com.greenfossil.thorium.Request,
                              actionResp: ActionResponse,
                              httpResp: HttpResponse
                             ): Try[HttpResponse] =
    for {
      respWithCookies <- Try(addCookiesToHttpResponse(getAllCookies(req, actionResp), httpResp))
      respWithHeaders <- Try(addHeadersToHttpResponse(responseHeader(req, actionResp), respWithCookies))
      httpResponse <- Try(addContentTypeToHttpResponse(contentTypeOpt(req, actionResp), respWithHeaders))
    } yield httpResponse

  private def _toHttpResponse(req: com.greenfossil.thorium.Request, actionResponse: ActionResponse): Try[HttpResponse] =
    Try:
      actionResponse match
        case null => throw new Exception(s"Null response in request [${req.uri}]")
        case hr: HttpResponse => hr
        case s: String => HttpResponse.of(s)
        case bytes: Array[Byte] => HttpResponse.of(HttpStatus.OK, Option(req.contentType).getOrElse(MediaType.ANY_TYPE), HttpData.wrap(bytes))
        case is: InputStream =>
          HttpResponse.of(
            ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, Option(req.contentType).getOrElse(MediaType.ANY_TYPE)),
            StreamMessage.fromOutputStream(os => Using.resources(is, os) { (is, os) => is.transferTo(os) })
          )
        case result: Result => _toHttpResponse(req, result.body).get

  def convertActionResponseToHttpResponse(req: com.greenfossil.thorium.Request, actionResp: ActionResponse): HttpResponse =
    serverLogger.debug(s"Convert ActionResponse to HttpResponse.")
    (for {
      
      httpResp <- _toHttpResponse(req, actionResp)
      httpRespWithHeaders <- _addHttpResponseHeaders(req, actionResp, httpResp)
    } yield httpRespWithHeaders)
      .fold(
        ex => {
          serverLogger.error("Invoke Action error", ex)
          HttpResponse.ofFailure(ex) // allow exceptionHandlerFunctions and serverErrorHandler to kick in
        },
        identity
      )