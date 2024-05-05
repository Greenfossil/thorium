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


import munit.IgnoreSuite

import scala.util.Random

@IgnoreSuite
class RecaptchaPlaywright_Stress_Suite extends munit.FunSuite:

  /**
   * Before running this testsuite, ensures that RecaptchaMainTestService is running.
   */
  import com.microsoft.playwright.*

  import java.util.concurrent.CountDownLatch
  import concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  test("playwright test") {
    val browserType = BrowserType.LaunchOptions() //.setTimeout(2000).setHeadless(false).setSlowMo(2000)
    val browserCnt = 5
    val loop = 2
    val browsersTup2: List[(Browser, Int)] =
      (1 to browserCnt).map(j => (Playwright.create().chromium().launch(browserType), j)).toList

    val startLatch = CountDownLatch(1)
    val endLatch = CountDownLatch(loop * browserCnt)

    browsersTup2.foreach { (browser, index) =>
      submitActionPerBrowser(browser, index, loop, startLatch, endLatch)
    }

    println(s"start all requests")
    startLatch.countDown()
    println("Waiting for all requests to finish")
    endLatch.await()
    browsersTup2.foreach(_._1.close())
    println("Terminating test")
  }

  private def submitActionPerBrowser(browser: Browser, i: Int, loop: Int, startLatch: CountDownLatch, endLatch: CountDownLatch): Future[Unit] =
    Future:
      startLatch.await()
      println(s"Starting creating submit page for browser $i...")
      1 to loop foreach { j =>
        val index: String = s"$i-$j"
        try {
          val bc = browser.newContext()
          val page = bc.newPage()
          page.navigate("http://localhost:8080/recaptcha/form-stress-test2")
          println(s"page index:${index} = ${page.url()}")
          val tag = s"N:$index"
          page.querySelector("input[name=blog]").fill(tag)
          val action = if Random().nextBoolean() then "submit" else "cancel"
          println(s"action = ${action}")
          val submitButtonSelector = s"button[data-action='$action']"
          page.querySelector(submitButtonSelector).click()
          endLatch.countDown()
        } catch {
          case ex: Throwable =>
            ex.printStackTrace()
            browser.close()
        }
      }
