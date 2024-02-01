package com.greenfossil.thorium

import com.greenfossil.thorium.decorators.RecaptchaGuardModule
import com.linecorp.armeria.server.ServiceRequestContext

import java.time.Duration

object RecaptchaMain:

  val recaptchaPathVerificationFn: ServiceRequestContext => Boolean =
    (ctx: ServiceRequestContext) =>
      ctx.path()
        .matches("/recaptcha/.+/guarded-form|/recaptcha/guarded-form") //prone to misconfigurations and not good for refactoring

  @main
  def recaptchaMain =
    val server = Server(8080)
      .addServices(RecaptchaServices)
      .addThreatGuardModule(RecaptchaGuardModule(recaptchaPathVerificationFn))
      .serverBuilderSetup(_.requestTimeout(Duration.ofHours(1)))
      .start()

    server.serviceConfigs foreach { c =>
      println(s"c.route() = ${c.route()}")
    }
    println(s"Server started... ${Thread.currentThread()}")