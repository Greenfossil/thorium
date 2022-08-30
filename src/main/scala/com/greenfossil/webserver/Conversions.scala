package com.greenfossil.webserver

import com.linecorp.armeria.common.{HttpResponse, MediaType}
import io.netty.util.AsciiString

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