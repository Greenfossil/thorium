package com.greenfossil.thorium

import com.greenfossil.thorium.decorators.RecaptchaGuardModule
import com.linecorp.armeria.server.ServiceRequestContext

import java.time.Duration

object RecaptchaMainTestService:

  def recaptchaPathVerificationFn(path: String, ctx: ServiceRequestContext):Boolean =
      //prone to misconfigurations and not good for refactoring
      path.matches("/recaptcha/guarded-form|/recaptcha/multipart-form")

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