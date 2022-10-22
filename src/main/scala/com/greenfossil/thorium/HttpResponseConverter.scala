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

  def convertActionResponseToHttpResponse(req: com.greenfossil.thorium.Request, actionResp: ActionResponse): HttpResponse =
    try {
      val httpResp = actionResp match
        case hr: HttpResponse => hr
        case s: String => HttpResponse.of(s)
        case bytes: Array[Byte] =>
          HttpResponse.of(HttpStatus.OK, Option(req.contentType).getOrElse(MediaType.ANY_TYPE), HttpData.wrap(bytes))
        case is: InputStream =>
          HttpResponse.of(
            ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, Option(req.contentType).getOrElse(MediaType.ANY_TYPE)),
            StreamMessage.fromOutputStream(os => Using.resources(is, os) { (is, os) => is.transferTo(os) }))
        case result: Result =>
          convertActionResponseToHttpResponse(req, result.body)

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