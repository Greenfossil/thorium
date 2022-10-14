package com.greenfossil.thorium

import com.greenfossil.data.mapping.Mapping
import com.linecorp.armeria.common.{HttpMethod, HttpResponse, MediaType}
import io.netty.util.AsciiString

import scala.util.Try

given Conversion[HttpResponse, Result] = Result(_)

extension (is: java.io.InputStream)
  def withHeaders(headers: (String | AsciiString, String)*): Result =
    Result(is).withHeaders(headers*)

  def as(contentType: MediaType): Result =
    Result(is).as(contentType)

extension (bytes: Array[Byte])
  def withHeaders(headers: (String | AsciiString, String)*): Result =
    Result(bytes).withHeaders(headers*)

  def as(contentType: MediaType): Result =
    Result(bytes).as(contentType)

extension (inline action: EssentialAction)
  inline def url: String = EndpointMcr(action).url

  inline def absoluteUrl(authority: String, secure: Boolean) =
    EndpointMcr(action).absoluteUrl(authority, secure)

  inline def absoluteUrl(using request: Request): String =
    EndpointMcr(action).absoluteUrl(using request)

extension (inline action: EssentialAction)
  inline def endpoint: Endpoint = EndpointMcr(action)

extension[A](field: Mapping[A])
  def bindFromRequest()(using request: Request): Mapping[A] =
    val queryData: List[(String, String)] =
      request.method match {
        case HttpMethod.POST | HttpMethod.PUT | HttpMethod.PATCH => Nil
        case _ => request.queryParamsList
      }

    request match
      //TODO - to be tested
      //      case req if req.asMultipartFormData.get().bodyPart.nonEmpty =>
      //        field.bind(req.asMultipartFormData.get().asFormUrlEncoded ++ queryData.groupMap(_._1)(_._2))

      case req if Try(req.asJson).isSuccess =>
        field.bind(request.asJson)

      case req =>
        field.bind(req.asFormUrlEncoded ++ queryData.groupMap(_._1)(_._2))