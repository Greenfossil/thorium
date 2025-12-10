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

package com.greenfossil.thorium.decorators

import com.greenfossil.thorium.*
import com.greenfossil.thorium.decorators.FirstResponderDecoratingFunction.SensitiveFieldConfig
import com.linecorp.armeria.common.multipart.Multipart
import com.linecorp.armeria.common.{Cookie, HttpRequest, HttpResponse, MediaType}
import com.linecorp.armeria.server.{DecoratingHttpServiceFunction, HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.time.ZoneId
import java.util.stream.Collectors
import scala.util.Try

object FirstResponderDecoratingFunction {

  private val firstResponderLogger = LoggerFactory.getLogger("com.greenfossil.thorium.first-responder")

  private val AcceptableMethods = List("POST", "PUT", "PATCH")

  private val AcceptableMediaTypes = List(MediaType.FORM_DATA, MediaType.JSON, MediaType.PLAIN_TEXT).map(_.subtype())

  /**
   * Configuration for masking sensitive fields in logs
   * @param sensitiveFieldNames Set of field name patterns to mask (case-insensitive substring matching)
   * @param maskValue The value to use for masking sensitive data
   * @param partialMaskFields Map of field names to (prefix, suffix) pairs for partial masking
   */
  case class SensitiveFieldConfig(
                                   /**
                                    * Fields that should be fully masked
                                    */
                                   sensitiveFieldNames: Set[String] = Set(
                                     "password", "passwd", "pwd",
                                     "secret", "token", "apikey", "api_key",
                                     "ssn", "social_security",
                                     "creditcard", "credit_card", "cardnumber", "card_number", "cvv", "cvc",
                                     "pin", "auth", "authorization"
                                   ),
                                   maskValue: String = "***REDACTED***",


                                   /**
                                    * Fields that should be partially masked instead of fully masked.
                                    * The map key is the field name pattern (case-insensitive substring matching),
                                    * The value is a tuple of (prefix length to show, suffix length to show)
                                    */
                                   partialMaskFields: Map[String, (prefix: Int, suffix: Int)] = Map(
                                     "email" -> (3, 4),
                                     "phone" -> (3, 2),
                                     "mobile" -> (3, 2)
                                   )
                                 )

  /**
   * Masks sensitive field values based on configuration
   */
  private def maskSensitiveValue(fieldName: String, value: String, config: SensitiveFieldConfig): String = {
    val lowerFieldName = fieldName.toLowerCase

    // Check if field should be completely masked
    if config.sensitiveFieldNames.exists(lowerFieldName.contains) then
      config.maskValue
    // Check if field should be partially masked
    else if config.partialMaskFields.keys.exists(lowerFieldName.contains) then
      val matchedKey = config.partialMaskFields.keys.find(lowerFieldName.contains).get
      val (prefix, suffix) = config.partialMaskFields(matchedKey)
      if value.length <= prefix + suffix then
        config.maskValue
      else
        s"${value.take(prefix)}***${value.takeRight(suffix)}"
    else
      value
  }

  /**
   * Masks sensitive data in URL-encoded form data (field1=value1&field2=value2)
   */
  private def maskFormData(body: String, config: SensitiveFieldConfig): String = {
    val FormDataPattern = """([^=&]+)=([^&]*)""".r
    FormDataPattern.replaceAllIn(body, m => {
      val fieldName = java.net.URLDecoder.decode(m.group(1), "UTF-8")
      val value = m.group(2)
      val maskedValue = maskSensitiveValue(fieldName, value, config)
      s"${m.group(1)}=$maskedValue"
    })
  }

  /**
   * Masks sensitive data in JSON (simple pattern-based approach)
   */
  private def maskJsonData(body: String, config: SensitiveFieldConfig): String = {
    val JsonFieldPattern = """"([^"]+)"\s*:\s*"([^"]*)"""".r
    JsonFieldPattern.replaceAllIn(body, m => {
      val fieldName = m.group(1)
      val value = m.group(2)
      val maskedValue = maskSensitiveValue(fieldName, value, config)
      s""""$fieldName":"$maskedValue""""
    })
  }

  /**
   * Masks sensitive data based on content type
   */
  private def maskBody(body: String, mediaType: MediaType, config: SensitiveFieldConfig): String = {
    if mediaType.is(MediaType.JSON) then
      maskJsonData(body, config)
    else if mediaType.is(MediaType.FORM_DATA) then
      maskFormData(body, config)
    else if mediaType.is(MediaType.PLAIN_TEXT) then
      // For plain text, apply both form and JSON masking patterns
      maskJsonData(maskFormData(body, config), config)
    else
      body
  }

  private def dumpFormDataBody(ctx: ServiceRequestContext, maxPlainTextContentLength: Int, sensitiveConfig: SensitiveFieldConfig): Unit =
    val mediaType = ctx.request().contentType()
    val contentLengthValue = ctx.request().headers().get("Content-Length")
    val contentLengthOpt = Try(contentLengthValue.toInt).toOption

    if mediaType == null then
      firstResponderLogger.warn("Dump body - null media type found, dump skip.")
    else if AcceptableMediaTypes.contains(mediaType.subtype()) then
      //For Plain_text, need to ensure length is not more that MaxContentLength
      if mediaType.subtype() != MediaType.PLAIN_TEXT.subtype() || contentLengthOpt.exists(_  <= maxPlainTextContentLength) then
        ctx.request()
          .aggregate()
          .thenAccept: aggReq =>
            ctx.blockingTaskExecutor().execute(() => {
              //Extract body content from FormData
              val body = aggReq.contentUtf8()
              val maskedBody = maskBody(body, mediaType, sensitiveConfig)
              firstResponderLogger.trace(s"Dump body - content-type:$mediaType, length:$contentLengthValue, body:$maskedBody")
            })
      else firstResponderLogger.trace(s"Dump body skip - content-type:$mediaType, content-length:$contentLengthValue, text-max-len:$maxPlainTextContentLength")
    else if mediaType.isMultipart then
      ctx.request()
        .aggregate()
        .thenAccept: aggReg =>
          ctx.blockingTaskExecutor().execute(() => {
            //Extract body content from Multipart
            Multipart.from(aggReg.toHttpRequest.toDuplicator.duplicate())
              .aggregate()
              .thenAccept: multipart =>
                val numParts = multipart.bodyParts().stream()
                  .filter{part =>
                    //PartContentLen Value if it is null, will use request content-length
                    val partContentLenValue = Option(part.headers().get("Content-Length")).getOrElse(contentLengthValue)
                    val partContentLengthOpt = Try(partContentLenValue.toInt).toOption
                    val withinContentLen = partContentLengthOpt.exists(_ <= maxPlainTextContentLength)
                    val acceptedType = AcceptableMediaTypes.contains(part.contentType().subtype())
                    //Log if Plain_text exceeds MaxContentLength
                    if part.contentType().subtype() == MediaType.PLAIN_TEXT.subtype() && !withinContentLen then
                      firstResponderLogger.trace(s"Dump body skip - content-type:${part.contentType()}, content-length:$partContentLenValue, text-max-len:$maxPlainTextContentLength")

                    acceptedType && withinContentLen
                  }
                  .collect(Collectors.toList)
                val mpBody = numParts.stream()
                  .map { part =>
                    val fieldName = part.name()
                    val value = part.contentUtf8()
                    val maskedValue = maskSensitiveValue(fieldName, value, sensitiveConfig)
                    s"$fieldName=$maskedValue"
                  }
                  .collect(Collectors.joining(";")).take(maxPlainTextContentLength)
                if !numParts.isEmpty then
                  val (partContentType, partContentLen) = numParts.stream().findFirst().map(part => part.contentType() -> part.headers().get("Content-Length")).get()
                  //PartContentLen Value if it is null, will use request content-length
                  val partLen = Try(partContentLen.toInt).getOrElse(contentLengthOpt.getOrElse(0))
                  firstResponderLogger.trace(s"Dump body - part-content-type:$partContentType, content-length:$partLen, parts:${numParts.size()}, partBody:$mpBody")
          })
    else
      firstResponderLogger.info(s"Dump body - unexpected content-type:$mediaType content-length:$contentLengthValue")
}

/**
 * This will be the first/last request within the control of Thorium.
 * This is use to setup Config/Session/Flash attributes which will be useful for all services
 *
 * @param configuration Application configuration for session, flash, and security settings
 * @param ignoreRequestFn Function to determine if a request should be ignored
 * @param maxPlainTextContentLength Maximum length for plain text content logging
 * @param sensitiveFieldConfig Configuration for masking sensitive fields in logs
 * @return
 */
class FirstResponderDecoratingFunction(val configuration: Configuration,
                                       val ignoreRequestFn: ServiceRequestContext => Boolean = _.request().uri().getPath.startsWith("/assets"),
                                       val maxPlainTextContentLength: Int = 2048,
                                       val sensitiveFieldConfig: SensitiveFieldConfig = SensitiveFieldConfig()
                                      ) extends DecoratingHttpServiceFunction:
  import FirstResponderDecoratingFunction.*

  override def serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    firstResponderLogger.debug(s"FirstResponder - remote:${ctx.remoteAddress()} method:${req.method()} request.uri:${req.uri()}")
    if firstResponderLogger.isTraceEnabled() then
      firstResponderLogger.trace(s"Headers:${req.headers().size()}")
      req.headers().forEach((key, value) => firstResponderLogger.trace(s"key:$key value:$value"))

    //Setup Session
    ctx.setAttr(RequestAttrs.Config, configuration)
    if ignoreRequestFn(ctx) then
      firstResponderLogger.trace(s"Ignore request and injected Configuration.")
      delegate.serve(ctx, req)
    else
      val cookies: Set[Cookie] =
        import scala.jdk.CollectionConverters.*
        ctx.request().headers().cookies().asScala.toSet

      //Setup TZ attr
      val tz = cookies.find(c => RequestAttrs.TZ.name() == c.name())
        .flatMap { c => Try(ZoneId.of(c.value())).toOption }
        .getOrElse(ZoneId.systemDefault())
      ctx.setAttr(RequestAttrs.TZ, tz)

      //Setup Session
      val session = cookies.find(c => c.name() == configuration.httpConfiguration.sessionConfig.cookieName).flatMap { c =>
        CookieUtil.decryptCookieValue(c, configuration.httpConfiguration.secretConfig.secret).map(Session(_))
      }.getOrElse(Session())
      ctx.setAttr(RequestAttrs.Session, session)

      //Setup Flash
      val flash = cookies.find(c => c.name() == configuration.httpConfiguration.flashConfig.cookieName).flatMap { c =>
        CookieUtil.decryptCookieValue(c, configuration.httpConfiguration.secretConfig.secret).map(Flash(_))
      }.getOrElse(Flash())
      ctx.setAttr(RequestAttrs.Flash, flash)

      firstResponderLogger.debug(s"Injected Configuration and extracted Cookies -TZ: $tz, Session:$session, Flash:$flash")
      val method = ctx.request().method().name()
      //Logs the body of AcceptableMethods and AcceptableMediaTypes, the body content is constrained by MaxContentLength
      if firstResponderLogger.isTraceEnabled() && AcceptableMethods.contains(method) then {
        dumpFormDataBody(ctx, maxPlainTextContentLength, sensitiveFieldConfig)
      }

      //Invoke delegate
      delegate.serve(ctx, req)