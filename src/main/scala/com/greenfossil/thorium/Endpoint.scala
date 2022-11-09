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

import com.linecorp.armeria.server.ServiceConfig

import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

case class Endpoint(path: String, method: String, queryParams: List[(String, Any)], pathPatternOpt: Option[String] = None):

  def url: String =
    //Query Param is expected to be UrlEncoded
    queryParams match
      case Nil => path
      case _ => path +
        queryParams
          .map(kv => s"${Endpoint.paramKeyValue(kv._1, kv._2)}")
          .mkString("?", "&", "")

  def absoluteUrl(authority: String, secure: Boolean): String =
    val protocol = if secure then "https" else "http"
    s"$protocol://$authority$url"

  def absoluteUrl(using request: Request): String =
    absoluteUrl(request.uriAuthority, request.secure)

  def prefixedUrl(using request: Request): String =
    import scala.jdk.CollectionConverters.*
    prefixedUrl(request.requestContext.config().server().serviceConfigs().asScala.toList)

  def prefixedUrl(serviceConfigs: Seq[ServiceConfig]): String =
    Endpoint.getPrefix(serviceConfigs, pathPatternOpt.getOrElse(path)).map{prefix => prefix + url }.getOrElse(url)

  def prefixedUrl2(using request: Request): String =
    import scala.jdk.CollectionConverters.*
    prefixedUrl2(request.path, request.requestContext.config().server().serviceConfigs().asScala.toList)

  def prefixedUrl2(requestPath: String, serviceConfigs: Seq[ServiceConfig]): String =
    Endpoint.getPrefix2(requestPath, serviceConfigs, pathPatternOpt.getOrElse(requestPath)).map { prefix => prefix + url }.getOrElse(url)

  override def toString: String = url

object Endpoint:

  def apply(path: String): Endpoint = new Endpoint(path, "GET", Nil)

  def apply(path: String, pathPattern: String): Endpoint =
    new Endpoint(path, "GET", Nil, Option(pathPattern))

  def paramKeyValue(name: String, value: Any): String =
    (name, value) match
      case (name, x :: Nil) => paramKeyValue(name, x)
      case (name, xs: Seq[Any]) => xs.map(x => s"${name}[]=${x.toString}").mkString("&")
      case (name, x: Any) => s"$name=${x.toString}"

  def paramKeyValueUrlEncoded(name: String, value: Any): String =
    (name, value) match
      case (name, x :: Nil) => paramKeyValueUrlEncoded(name, x)
      case (name, xs: Seq[Any]) => xs.map(x => s"${urlencode(name)}[]=${urlencode(x.toString)}").mkString("&")
      case (name, x: Any) => s"${urlencode(name)}=${urlencode(x.toString)}"

  def urlencode(value: String): String =
    java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString)

  def getPrefix(serviceConfigs: Seq[ServiceConfig], rawPathPattern: String): Option[String] =
    getPrefix2("", serviceConfigs, rawPathPattern)

  def getPrefix2(requestPath: String, serviceConfigs: Seq[ServiceConfig], rawPathPattern: String): Option[String] =
    //1. Compute Redirect Endpoint prefix that matches incoming Request
    val isPrefix = rawPathPattern.startsWith("prefix:")
    val epPathPattern = //This would be forwardEndpoint Annotated path pattern
      if isPrefix
      then rawPathPattern.replaceAll("prefix:", "") + "/*"
      else rawPathPattern.replaceAll("regex:|glob:", "")

    val matchedConfigs = serviceConfigs
      .filter { serviceConfig =>
        val configRoutePattern = serviceConfig.route().patternString()
        val isConfigRoute = configRoutePattern.endsWith(epPathPattern)
        val configRoutePrefix =
          val pat = configRoutePattern.replaceAll(epPathPattern, "")
          if !isPrefix then pat
          else pat.replaceAll("\\*", ".+")
        val matchedRequestPrefix =
          //if requestPath is empty  then matchedRequestPrefix is set as true
          requestPath.isEmpty || configRoutePrefix.nonEmpty &&
            (if isPrefix then requestPath.matches(configRoutePrefix) else  requestPath.startsWith(configRoutePrefix))

        isConfigRoute && matchedRequestPrefix
      }
    matchedConfigs.lastOption.map(serviceConfig => {
      //Convert the matched serviceConfig to the prefix
      val configRoutePattern = serviceConfig.route().patternString()
      val prefix = configRoutePattern.replaceAll(Pattern.quote(epPathPattern), "")
      if prefix.lastOption.contains('/') then prefix.init else prefix
    })

