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

import com.greenfossil.data.mapping.Mapping
import com.linecorp.armeria.common.multipart.MultipartFile
import com.linecorp.armeria.common.{HttpMethod, MediaType}
import org.overviewproject.mime_types.MimeTypeDetector

import java.io.InputStream
import java.net.{CookieStore, URI}


extension (inline action: EssentialAction)

  inline def absoluteUrl(authority: String, secure: Boolean) =
    EndpointMcr(action).absoluteUrl(authority, secure)

  inline def absoluteUrl(using request: Request): String =
    EndpointMcr(action).absoluteUrl(using request)

  inline def endpoint: Endpoint = EndpointMcr(action)

extension[A](field: Mapping[A])
  def bindFromRequest()(using request: Request): Mapping[A] =
    request match
      case req if Option(req.contentType).exists(_.isJson) =>
        field.bind(request.asJson)

      case req =>
        //QueryParams for POST,PUT,PATCH will be not be used for binding
        val queryParams: List[(String, String)] =
          request.method match {
            case HttpMethod.POST | HttpMethod.PUT | HttpMethod.PATCH => Nil
            case _ => request.queryParamsList
          }
        field.bind(req.asFormUrlEncoded ++ queryParams.groupMap(_._1)(_._2))

val mimeTypeDetector = new MimeTypeDetector()

extension(mpFile: MultipartFile)
  def sizeInGB: Long = mpFile.file().length() / 1024 / 1024 / 1024
  def sizeInMB: Long = mpFile.file().length() / 1024 / 1024
  def sizeInKB: Long = mpFile.file().length() / 1024
  def sizeInBytes: Long = mpFile.file().length()

  def contentType: MediaType = MediaType.parse(mimeTypeDetector.detectMimeType(mpFile.path()))

  def inputStream: InputStream = mpFile.path().toUri.toURL.openStream()

extension(cs: java.net.CookieStore)

  /**
   * Add an Armeria cookie to java.net.CookieStore
   * This is meant for client testing only. Do not suse for production.
   * @param host
   * @param cookie
   */
  def add(host: String, cookie: com.linecorp.armeria.common.Cookie)  =
    val uri = URI.create(host)
    val httpCookie = java.net.HttpCookie(cookie.name(), cookie.value())
    httpCookie.setSecure(cookie.isSecure)
    httpCookie.setPath(cookie.path())
    httpCookie.setHttpOnly(cookie.isHttpOnly)
    httpCookie.setMaxAge(httpCookie.getMaxAge)
    if cookie.isHostOnly then httpCookie.setHttpOnly(true)
    if cookie.domain() != null && !cookie.domain().isBlank then httpCookie.setDomain(cookie.domain())

    cs.add(uri, httpCookie)

extension (l: Long) def humanize: String =
  if l < 1024 then s"$l B"
  else
    val z = (63 - java.lang.Long.numberOfLeadingZeros(l)) / 10
    f"${(l * 1.0) / (1L << (z * 10))}%.1f ${" KMGTPE".charAt(z)}%sB"