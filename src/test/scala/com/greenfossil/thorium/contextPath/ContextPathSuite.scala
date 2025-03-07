package com.greenfossil.thorium.contextPath

class ContextPathSuite extends munit.FunSuite:

  test("ContextPath compilation") {
    import com.linecorp.armeria.common.HttpResponse
    import com.linecorp.armeria.server.Server

    /*
     * Inssue raised -
     * https://github.com/scala/scala3/issues/21797
     * https://github.com/line/armeria/issues/5947
     */
    val server = Server.builder()
      .contextPath("/v1")
      .service("/", (ctx, req) => HttpResponse.of("Hello, world"))
      .and()
      .build()

    val started = server.start()
    started.get()
    server.stop()
  }
