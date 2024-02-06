/*
 *  Copyright 2022 Greenfossil Pte Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.greenfossil.thorium.decorators

import com.greenfossil.thorium.FormUrlEncodedParser
import com.linecorp.armeria.common.multipart.Multipart
import com.linecorp.armeria.common.{HttpMethod, HttpRequest, MediaType}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.Logger

import java.util.concurrent.CompletableFuture
import scala.util.Using

class AndThreatGuardModule(moduleA: ThreatGuardModule, moduleB: ThreatGuardModule) extends ThreatGuardModule:

  override protected val logger: Logger = null

  override def isSafe(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): CompletableFuture[Boolean] =
    moduleA.isSafe(delegate, ctx, req)
      .thenCombine(moduleB.isSafe(delegate, ctx, req), (isSafeA, isSafeB) => isSafeA && isSafeB)

class OrThreatGuardModule(moduleA: ThreatGuardModule, moduleB: ThreatGuardModule) extends ThreatGuardModule:
  override protected val logger: Logger = null

  override def isSafe(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): CompletableFuture[Boolean] =
    moduleA.isSafe(delegate, ctx, req)
      .thenCombine(moduleB.isSafe(delegate, ctx, req), (isSafeA, isSafeB) => isSafeA || isSafeB)

trait ThreatGuardModule:

  protected val logger: Logger

  //Need to allow, on success response callback
  def isSafe(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): CompletableFuture[Boolean]

  def and(nextModule: ThreatGuardModule): ThreatGuardModule = AndThreatGuardModule(this, nextModule)

  def or(nextModule: ThreatGuardModule): ThreatGuardModule = OrThreatGuardModule(this, nextModule)

  def isAssetPath(ctx: ServiceRequestContext): Boolean =
    ctx.request().path().startsWith("/assets") && HttpMethod.GET == ctx.method()

  protected def extractTokenValue(ctx: ServiceRequestContext, tokenName: String): CompletableFuture[String] =
    val mediaType = ctx.request().contentType()
    val futureResp = new CompletableFuture[String]()
    logger.debug(s"Extract token from request - content-type:${ctx.request().contentType()}")
    if mediaType == null then
      logger.warn("Null media type found. Treat token as null")
      futureResp.complete(null)
    else if mediaType.is(MediaType.FORM_DATA) then
      ctx.request()
        .aggregate()
        .thenAccept: aggReq =>
          ctx.blockingTaskExecutor().execute(() => {
            //Extract token from FormData
            val form = FormUrlEncodedParser.parse(aggReq.contentUtf8())
            val token = form.get(tokenName).flatMap(_.find(!_.isBlank)).orNull
            logger.trace(s"Found Token:$token, content-type:$mediaType.")
            futureResp.complete(token)
          })
    else if mediaType.isMultipart then
      ctx.request()
        .aggregate()
        .thenAccept: aggReg =>
          ctx.blockingTaskExecutor().execute(() => {
            //Extract token from Multipart
            Multipart.from(aggReg.toHttpRequest.toDuplicator.duplicate())
              .aggregate()
              .thenAccept: multipart =>
                val partOpt = Using.resource(multipart.fields(tokenName).stream())(_.filter(!_.contentUtf8().isBlank).findFirst())
                val token = if partOpt.isEmpty then null else partOpt.get().contentUtf8()
                logger.trace(s"Found Token:$token, content-type:$mediaType.")
                futureResp.complete(token)
          })

    else {
      logger.info(s"Token found unsupported for content-type:$mediaType.")
      futureResp.complete(null)
    }
    futureResp
