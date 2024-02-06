package com.greenfossil.thorium

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.server.annotation.{Get, Post}

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

  /**
   * explicit Recaptcha  verification Recaptcha#verify done in the post method
   * @return
   */
  @Get("/recaptcha/form")
  def recaptchaForm: Action = Action { implicit request =>
    RecaptchaFormPage(postRecaptchaForm.endpoint, isMultipart = false)
  }

  /**
   * explicit Recaptcha  verification Recaptcha#verify done in the post method
   * @return
   */
  @Post("/recaptcha/form")
  def postRecaptchaForm: Action = Action { implicit request =>
    Recaptcha.verify
      .fold(
        ex => Unauthorized(s"Recaptcha exception - ${ex.getMessage}"),
        recaptcha =>
          if recaptcha.success then Redirect(recaptchaForm).flashing("success" -> s"Recaptcha response:[$recaptcha]")
          else Unauthorized(s"Unauthorize access - $recaptcha")
      )
  }

  /**
   * explicit Recaptcha  verification Recaptcha#onVerify done in the post method
   *
   * @return
   */
  @Get("/recaptcha/form2")
  def recaptchaForm2: Action = Action { implicit request =>
    RecaptchaFormPage(postRecaptchaForm2.endpoint, isMultipart = false)
  }

  /**
   * explicit Recaptcha  verification Recaptcha#onVerify done in the post method
   *
   * @return
   */
  @Post("/recaptcha/form2")
  def postRecaptchaForm2: Action = Action { implicit request =>
    Recaptcha.onVerified(r => r.success){
      Redirect(recaptchaForm2).flashing("success" -> s"Recaptcha response success:${request.recaptchaResponse}")
    }
  }

  /**
   * RecaptchaGuardModule
   *
   * @return
   */
  @Get("/recaptcha/guarded-form")
  def recaptchaGuardedForm: Action = Action { implicit request =>
    RecaptchaFormPage(postRecaptchaGuardedForm.endpoint, isMultipart = false)
  }

  /**
   * RecaptchaGuardModule
   *
   * @return
   */
  @Post("/recaptcha/guarded-form")
  def postRecaptchaGuardedForm: Action = Action { implicit request =>
    println(s"request.recaptchaResponse = ${request.recaptchaResponse}")
    Redirect(recaptchaGuardedForm).flashing("success" -> s"Recaptcha response success:[${request.recaptchaResponse}]")
  }


  /**
   * RecaptchaGuardModule
   *
   * @return
   */
  @Get("/recaptcha/multipart-form")
  def recaptchaMultipartForm: Action = Action { implicit request =>
    RecaptchaFormPage(postRecaptchaMulitpartForm.endpoint, isMultipart = true)
  }

  /**
   * RecaptchaGuardModule
   *
   * @return
   */
  @Post("/recaptcha/multipart-form")
  def postRecaptchaMulitpartForm: Action = Action.multipart { implicit request =>
    println(s"request.recaptchaResponse = ${request.recaptchaResponse}")
    Redirect(recaptchaMultipartForm).flashing("success" -> s"Recaptcha response success:[${request.recaptchaResponse}]")
  }