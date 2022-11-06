import com.linecorp.armeria.server.*
import com.linecorp.armeria.server.annotation.*
import com.linecorp.armeria.common.*

Server.builder()
  .service(Services.s0)
  .serviceUnder("/api", Services.s1)
  .build()

object Services:

  @Get("/s0")
  def s0(ctx: ServiceRequestContext, request: HttpRequest): HttpResponse =
    val loc = "s1" //1. how to create a redirect path based on a request path
    HttpResponse.ofRedirect(loc)


  @Get("/s1")
  def s1(ctx: ServiceRequestContext, request: HttpRequest): HttpResponse =
    HttpResponse.of("Hello World")
