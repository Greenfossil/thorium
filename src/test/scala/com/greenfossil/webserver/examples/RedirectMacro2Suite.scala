package com.greenfossil.webserver.examples

import com.greenfossil.webserver.*
import com.linecorp.armeria.server.annotation.{Get, Param}

@main def redirect2Main =
  val server = WebServer(8080)
    .addServices(Redirect3Services)
    .addServices(Redirect4Services)
    .start()

object Redirect3Services extends Controller {

  //curl http://localhost:8080/action1 -L
  @Get("/fn1")
  def fn1 =
    //Redirect with Ref and Constant
    val time = java.time.LocalDate.now.toString
    val ep = EndpointMcr(fn2(time, "World2!")(using null))
    Redirect(ep)

  @Get("/fn2/:data/:msg")
  def fn2(@Param data: String, @Param msg: String)(using request: Request) =
    s"Hello value:$data, msg:$msg - req${request.path}"
}

object Redirect4Services extends Controller {

  /*
   * curl http://localhost:8080/fn3/1 -L
   */
  @Get("/fn3/:id")
  def fn3(@Param id: Long)(using req: Request) =
    Redirect(EndpointMcr(fn4(id)))

  @Get("/fn4/:id")
  def fn4(@Param id: Long)(using req: Request) =
    //Redirect with Ref - name, and inline
    def name = java.net.URLEncoder.encode(s"SpaceId#${id}!","utf-8") //Note: '#' must be urlencoded
    Redirect(EndpointMcr(Redirect3Services.fn2(name, java.time.LocalDateTime.now.toString)))

}
