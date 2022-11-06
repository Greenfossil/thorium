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
    prefixedUrl(request.path,  request.requestContext.config().server().serviceConfigs().asScala.toList)

  def prefixedUrl(requestPath: String, serviceConfigs: List[ServiceConfig]): String =
  //1. Compute Redirect Endpoint prefix that matches incoming Request
    serviceConfigs
      .find(serviceConfig =>
        pathPatternOpt.exists { pathPattern => //This would be forwardEndpoint Annotated path pattern
          val configRoutePattern = serviceConfig.route().patternString()
          val configRoutePrefix = configRoutePattern.replaceAll(pathPattern, "")
          val configRoutePatternEndsWithForwardEpPattern = configRoutePattern.endsWith(pathPattern)
          val reqStartsWithConfigRoutePrefix = configRoutePrefix.nonEmpty && requestPath.startsWith(configRoutePrefix)
          configRoutePatternEndsWithForwardEpPattern && reqStartsWithConfigRoutePrefix
        }
      )
      .map(serviceConfig => {
        //Convert the matched serviceConfig to the prefix
        val prefix = serviceConfig.route().patternString().replaceAll(pathPatternOpt.get, "")
        prefix + url
      }).getOrElse(url)

  override def toString: String = url

object Endpoint:

  def apply(path: String): Endpoint = new Endpoint(path, "GET", Nil)

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
