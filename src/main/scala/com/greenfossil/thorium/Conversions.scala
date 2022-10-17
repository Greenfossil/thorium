package com.greenfossil.thorium

import com.greenfossil.data.mapping.Mapping
import com.linecorp.armeria.common.multipart.MultipartFile
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
    request match
      case req if req.contentType == MediaType.JSON =>
        field.bind(request.asJson)

      case req =>
        //QueryParams for POST,PUT,PATCH will be not be used for binding
        val queryParams: List[(String, String)] =
          request.method match {
            case HttpMethod.POST | HttpMethod.PUT | HttpMethod.PATCH => Nil
            case _ => request.queryParamsList
          }
        field.bind(req.asFormUrlEncoded ++ queryParams.groupMap(_._1)(_._2))
        
extension(mpFile: MultipartFile)
  def fileSizeGB: Double = mpFile.file().length().toDouble / 1000 / 1000 / 1000
  def fileSizeMB: Double = mpFile.file().length().toDouble / 1000 / 1000
  def fileSizeByte: Long = mpFile.file().length()   