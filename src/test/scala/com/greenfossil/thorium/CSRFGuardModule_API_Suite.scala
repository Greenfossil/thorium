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

import com.greenfossil.thorium.decorators.CSRFGuardModule

class CSRFGuardModule_API_Suite extends munit.FunSuite:

  test("csrfToken - hmac with different key") {
    (for {
      csrfToken <- CSRFGuardModule.generateCSRFToken("ABCD", "HmacSHA256", "1234")
    } yield
      val verifyResult = CSRFGuardModule.verifyHmac(csrfToken, "ABCD1", "HmacSHA256")
      println(s"csrfToken = ${csrfToken}")
      println(s"verifyResult = ${verifyResult}")
      verifyResult
      )
      .fold(
        ex => fail("Fail", ex),
        success => assert(!success)
      )
  }

  test("csrfToken - with same key") {
    (for {
      csrfToken <- CSRFGuardModule.generateCSRFToken("ABCD", "HmacSHA256", "1234")
    } yield
      val verifyResult = CSRFGuardModule.verifyHmac(csrfToken, "ABCD", "HmacSHA256")
      println(s"csrfToken = ${csrfToken}")
      println(s"verifyResult = ${verifyResult}")
      verifyResult
      )
      .fold(
        ex => fail("Fail", ex),
        success => assert(success)
      )
  }


