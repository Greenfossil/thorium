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

package experiment.endpoint

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.server.annotation.*

object TestActionController extends Controller {

  @Get("/action1")
  def action1: Action = Action { implicit request =>
    Redirect(action2("homer"))
  }

  @Get("/action2/:name")
  def action2(@Param name: String) = Action{implicit request =>
    s"Hello $name"
  }

  @Get("/action3")
  def action3 = Action { implicit request =>
    Redirect(action1)
  }

  @Get("/nested")
  def nestedPath = Action {implicit request =>
    Redirect(getPath(action3))
  }

  //getPath needs to be declared as inline and argumentment as inline as well
  inline def getPath(inline action: EssentialAction): Endpoint =
    EndpointMcr(action)

}

class MethodSignatureSuite extends munit.FunSuite {

  test("endpoint"){
    val action2Endpoint = EndpointMcr(TestActionController.action2("hello"))
    assertEquals(action2Endpoint.url, "/action2/hello")

    val getPathEndpoint = TestActionController.getPath(TestActionController.action3)
    assertNoDiff(getPathEndpoint.url, "/action3")
  }

}
