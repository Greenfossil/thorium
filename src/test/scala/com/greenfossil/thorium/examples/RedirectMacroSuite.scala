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

package com.greenfossil.thorium.examples

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Param}

@main def redirectMain =
  val server = Server(8080)
    .addServices(Redirect1Services)
    .addServices(Redirect2Services)
    .start()

object Redirect1Services extends Controller {

  //curl http://localhost:8080/action1 -L
  @Get("/action1")
  def action1 = Action { request =>
    //Redirect with Ref and Constant
    def time = java.time.LocalDate.now.toString
    Redirect(action2(time, "World!"))
  }

  @Get("/action2/:value/:msg")
  def action2(@Param value: String, @Param msg: String) = Action { request =>
    s"Hello value:$value, msg:$msg"
  }
}

object Redirect2Services extends Controller {

  /*
   * curl http://localhost:8080/action3 -L
   */
  @Get("/action3")
  def action3 = Action { request =>
    Redirect(action4)
  }

  @Get("/action4")
  def action4 = Action { request =>
    //Redirect with Ref - name, and inline
    def name = "Space!"
    Redirect(Redirect1Services.action2(name, java.time.LocalDateTime.now.toString))
  }

}
