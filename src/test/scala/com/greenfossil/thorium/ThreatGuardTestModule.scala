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

package com.greenfossil.thorium

import com.greenfossil.thorium.decorators.ThreatGuardModule
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.CompletableFuture

class ThreatGuardTestModule(tokenName: String, tokenValue:String) extends ThreatGuardModule:
  override protected val logger: Logger = LoggerFactory.getLogger("threatguard-test-module")
  override def isSafe(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): CompletableFuture[Boolean] =
    extractTokenValue(ctx, tokenName)
      .thenApply: token =>
        token == tokenValue
