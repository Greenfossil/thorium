package experiment.endpoint

import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.*

object TestActionController extends Controller {

  @Get("/action1")
  def action1: Action = Action { implicit request =>
    Redirect(action2("homer"))
  }

  @Get("/action2/:name")
  def action2(name: String) = Action{implicit request =>
    s"Hello $name"
  }

  @Get("/action3")
  def action3 = Action { implicit request =>
    Redirect(action1)
  }

  @Get("/nested")
  def nestedPath = Action {implicit request =>
    getPath(action3)
  }

  def getPath(action: Action): String = ???
//    EndpointMcr(action).toString

}

class MethodSignatureSuite extends munit.FunSuite {

  test("endpoint"){
    val path = EndpointMcr(TestActionController.action2("hello"))
    println(s"Path===> ${path}")
  }

}
