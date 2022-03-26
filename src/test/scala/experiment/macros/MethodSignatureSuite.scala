package experiment.macros

import com.greenfossil.webserver.{*, given}
import com.linecorp.armeria.server.annotation.*

import experiment.macros.MethodSignatureMacro.*

object TestActionController extends Controller {

  @Get("/action1")
  def action1: Action = Action { implicit request =>
    Redirect("path")
  }

  @Get("/action2/:name")
  def action2(name: String) = Action{implicit request =>
    s"Hello $name"
  }

  @Get("/action3")
  def action3 = Action { implicit request =>
    Redirect("path")
  }

}

class MethodSignatureSuite extends munit.FunSuite {

  test("method signature") {

//    val path = MCall(TestActionController.action2("hello"))
    val path = MCall(TestActionController.action2("hello"))
    println(s"Path===> ${path}")
  }

}
