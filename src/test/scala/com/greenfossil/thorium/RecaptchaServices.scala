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

  @Get("/recaptcha/form-stress-test")
  def recaptchaFormStressTest: Action = Action { implicit request =>
    RecaptchaFormPage(postRecaptchaFormStressTest.endpoint, isMultipart = false)
  }

  /**
   * No Recaptcha guarding
   * @return
   */
  @Post("/recaptcha/form-stress-test")
  def postRecaptchaFormStressTest: Action = Action { implicit request =>
      import com.greenfossil.data.mapping.Mapping.*
      tuple("blog" -> text, "action" ->  text).bindFromRequest()
        .fold(
          error => BadRequest("Bad Request"),
          (blog, action) =>
            val msg = s"Success - request #${blog} action:$action"
            println(msg)
            msg
        )
  }

  @Get("/recaptcha/form-stress-test2")
  def recaptchaFormStressTest2: Action = Action { implicit request =>
    RecaptchaFormPage(postRecaptchaFormStressTest2.endpoint, isMultipart = false, useRecaptcha = true)
  }

  /**
   * explicit Recaptcha  verification Recaptcha#onVerify done in the post method
   * @return
   */
  @Post("/recaptcha/form-stress-test2")
  def postRecaptchaFormStressTest2: Action = Action { implicit request =>
    Recaptcha.onVerified(r => r.success) {
      import com.greenfossil.data.mapping.Mapping.*
      tuple("blog" -> text, "g-recaptcha-response" ->  seq[String]).bindFromRequest()
        .fold(
          error =>
            println(s"Form error = ${error}")
            BadRequest("Bad Request " + error),
          (blog, xs) =>
            val msg = s"Success - request #${blog} g-recaptcha-response:${xs.mkString}"
            println(msg)
            msg
        )
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