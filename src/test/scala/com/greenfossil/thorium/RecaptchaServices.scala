package com.greenfossil.thorium

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Param, Post}

object RecaptchaServices:
  /*
   * Note: recaptcha sign up information is in the link below, and copied here http://www.google.com/recaptcha/admin
   * https://developers.google.com/recaptcha
   * https://cloud.google.com/recaptcha-enterprise/docs/setup-overview-web
   * https://www.youtube.com/watch?v=dyA_Pbtbn_E - How to use Google reCaptcha v3 in your Spring Boot application
   * vue-recaptcha v3
   * Recaptcha V2 Invisible
   * Client side integration - https://developers.google.com/recaptcha/docs/invisible
   * Server side integration - https://developers.google.com/recaptcha/docs/verify
   */

  @Get("/recaptcha/form")
  def recaptchaForm: Action = Action { implicit request =>
    RecaptchaFormPage(request.config.httpConfiguration.recaptchaConfig.siteKey, postRecaptchaForm.endpoint)
  }

  @Post("/recaptcha/form")
  def postRecaptchaForm: Action = Action { implicit request =>
    Recaptcha.verify
      .fold(
        ex => Redirect(recaptchaForm).flashing("error" -> s"Recaptcha exception - ${ex.getMessage}"),
        recaptcha =>
          val errorType = if recaptcha.fail then "error" else "success"
          Redirect(recaptchaForm).flashing(errorType -> s"Recaptcha response:[$recaptcha]")
      )
  }

  @Get("/recaptcha/form2")
  def recaptchaForm2: Action = Action { implicit request =>
    RecaptchaFormPage(request.config.httpConfiguration.recaptchaConfig.siteKey, postRecaptchaForm2.endpoint)
  }

  @Post("/recaptcha/form2")
  def postRecaptchaForm2: Action = Action { implicit request =>
    Recaptcha.onVerified(r => r.success){
        Redirect(recaptchaForm2).flashing("success" -> s"Recaptcha response success:${request.recaptchaResponse}")
      }
  }

  @Get("/recaptcha/guarded-form")
  def recaptchaGuardedForm: Action = Action { implicit request =>
    RecaptchaFormPage(request.config.httpConfiguration.recaptchaConfig.siteKey, postRecaptchaGuardedForm.endpoint)
  }

  @Post("/recaptcha/guarded-form")
  def postRecaptchaGuardedForm: Action = Action { implicit request =>
    println(s"request.recaptchaResponse = ${request.recaptchaResponse}")
    Redirect(recaptchaGuardedForm).flashing("success" -> s"Recaptcha response success:[${request.recaptchaResponse}]")
  }

  @Get("/recaptcha/:version/guarded-form")
  def recaptchaGuardedForm2(@Param version: String): Action = Action { implicit request =>
    RecaptchaFormPage(request.config.httpConfiguration.recaptchaConfig.siteKey, postRecaptchaGuardedForm2(version).endpoint)
  }

  @Post("/recaptcha/:version/guarded-form")
  def postRecaptchaGuardedForm2(@Param version: String): Action = Action { implicit request =>
    println(s"request.recaptchaResponse = ${request.recaptchaResponse}")
    Redirect(recaptchaGuardedForm2(version)).flashing("error" -> s"Recaptcha ${version} response success:[${request.recaptchaResponse}]")
  }
