package experiment.endpoint

import com.greenfossil.webserver.{*, given}
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
