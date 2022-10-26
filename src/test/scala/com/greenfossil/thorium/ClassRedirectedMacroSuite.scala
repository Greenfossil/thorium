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

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.server.annotation.*
/*
 *  Class annotated path is currently not supported - Need to lift this restriction
 */
object SingletonService extends Controller {

//    @Get("/classRefAction")
//    def classRefAction = Action { request =>
//      //Reference class' action
//      Redirect[ClassServices](_.action1("Howdy!"))
//    }

}

//class ClassServices extends Controller {
//  @Get("/action1/:name")
//  def action1(@Param name: String) = Action { request =>
//    Redirect(action2("Class Service"))
//  }
//
//  @Get("/action2/:msg")
//  def action2(@Param msg: String) = Action { request =>
//    Ok(s"Class $msg")
//  }
//
//}

class ClassRedirectedMacroSuite extends munit.FunSuite{

}
