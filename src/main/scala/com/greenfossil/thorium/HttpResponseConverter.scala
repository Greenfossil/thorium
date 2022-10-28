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

import com.linecorp.armeria.common.{Cookie, HttpData, HttpHeaderNames, HttpResponse, HttpStatus, MediaType, ResponseHeaders}
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

  private def getAllCookies(req: Request, actionResp: ActionResponse): Seq[Cookie] =
    val (newCookies, newSessionOpt, newFlashOpt) =
      actionResp match
        case result: Result => (result.newCookies, result.newSessionOpt, result.newFlashOpt)
        case _ => (Nil, None, None) //FIXME
    (getNewSessionCookie(req, newSessionOpt) ++ getNewFlashCookie(req, newFlashOpt)).toList ++ newCookies

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
    newFlashOpt.flatMap { newFlash =>
      CookieUtil.bakeFlashCookie(newFlash)(using req) //Create a new flash cookie
    }.orElse {
      //Expire the current flash cookie
      if req.flash.isEmpty then None
      else Some(CookieUtil.bakeDiscardCookie(req.httpConfiguration.flashConfig.cookieName)(using req))
    }

  private def _toHttpResponse(req: com.greenfossil.thorium.Request,
                              actionResp: ActionResponse,
                              httpResp: HttpResponse
                             ) =
    val result: Try[HttpResponse] = for {
      respWithCookies <- Try(addCookiesToHttpResponse(getAllCookies(req, actionResp), httpResp))
      respWithHeaders <- Try(addHeadersToHttpResponse(responseHeader(req, actionResp), respWithCookies))
      respWithContentType <- Try(addContentTypeToHttpResponse(contentTypeOpt(req, actionResp), respWithHeaders))
    } yield respWithContentType

    result.getOrElse(httpResp)

  def convertActionResponseToHttpResponse(req: com.greenfossil.thorium.Request, actionResp: ActionResponse): HttpResponse =
    try {
      val httpResp = actionResp match
        case hr: HttpResponse => _toHttpResponse(req, actionResp, hr)
        case s: String => _toHttpResponse(req, actionResp, HttpResponse.of(s))
        case bytes: Array[Byte] =>
          _toHttpResponse(req, actionResp,
            HttpResponse.of(HttpStatus.OK, Option(req.contentType).getOrElse(MediaType.ANY_TYPE), HttpData.wrap(bytes))
          )
        case is: InputStream =>
          _toHttpResponse(req, actionResp,
            HttpResponse.of(
              ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, Option(req.contentType).getOrElse(MediaType.ANY_TYPE)),
              StreamMessage.fromOutputStream(os => Using.resources(is, os) { (is, os) => is.transferTo(os) }))
          )
        case result: Result =>
          _toHttpResponse(req, actionResp, convertActionResponseToHttpResponse(req, result.body))
        case null =>
          HttpResponse.ofFailure(new Exception(s"Null response in request [${req.uri}]"))

      val result: Try[HttpResponse] = for {
        respWithCookies <- Try(addCookiesToHttpResponse(getAllCookies(req, actionResp), httpResp))
        respWithHeaders <- Try(addHeadersToHttpResponse(responseHeader(req, actionResp), respWithCookies))
        respWithContentType <- Try(addContentTypeToHttpResponse(contentTypeOpt(req, actionResp), respWithHeaders))
      } yield respWithContentType

      result.getOrElse(httpResp)
    } catch {
      case t: Throwable =>
        actionLogger.error("Invoke Action error", t)
        HttpResponse.ofFailure(t) // allow exceptionHandlerFunctions and serverErrorHandler to kick in
    }