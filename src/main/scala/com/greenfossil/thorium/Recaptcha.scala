package com.greenfossil.thorium

import com.greenfossil.commons.json.*
import com.greenfossil.data.mapping.Mapping
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.{MediaType, QueryParams}
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.util.Try

object Recaptcha:
  private val logger = LoggerFactory.getLogger("com.greenfossil.elementum.recaptcha")

  def onVerified(using request: Request)(predicate: Recaptcha => Boolean)(result: => ActionResponse): ActionResponse =
    verify.fold(
      ex => Unauthorized("Access denied."),
      recaptcha =>
        if !predicate(recaptcha) then
          logger.warn(s"Unauthorized access. recaptcha:${recaptcha}, method:${request.method}, uri:${request.uri} path:${request.path} content-type:${request.contentType}")
          Unauthorized("Access denied.")
        else
          request.requestContext.setAttr(RequestAttrs.RecaptchaResponse, recaptcha)
          result

    )


  def verify(using request: Request): Try[Recaptcha] =
    verify(request.config.httpConfiguration.recaptchaConfig.secretKey)

  def verify(secretKey: String)(using request: Request): Try[Recaptcha] =
    Mapping("g-recaptcha-response", seq[String])
      .bindFromRequest()
      .fold(
        ex => Try(throw new IllegalArgumentException(s"Binding exception - ${ex.errors.map(_.message).mkString(",")}")),
        xs => siteVerify(xs, secretKey)
      )

  def siteVerify(captchaValues: Seq[String], recaptchaSecret: String): Try[Recaptcha] =
    captchaValues.find(_.nonEmpty) match
      case Some(token) => siteVerify(token, recaptchaSecret)
      case None => Try(throw new IllegalArgumentException("No recaptcha token found"))


  /**
   * https://developers.google.com/recaptcha/docs/verify
   *
   * @param captchaValue
   * @param recaptchaSecret
   * @return - json
   *         Recaptcha V2 response format
   *         {
   *         "success": true|false,
   *         "challenge_ts": timestamp,  // timestamp of the challenge load (ISO format yyyy-MM-dd'T'HH:mm:ssZZ)
   *         "hostname": string,         // the hostname of the site where the reCAPTCHA was solved
   *         "error-codes": [...]        // optional
   *         }
   *
   *         Recaptcha V3 response format
   *         {
   *         "success": true|false,      // whether this request was a valid reCAPTCHA token for your site
   *         "score": number             // the score for this request (0.0 - 1.0)
   *         "action": string            // the action name for this request (important to verify)
   *         "challenge_ts": timestamp,  // timestamp of the challenge load (ISO format yyyy-MM-dd'T'HH:mm:ssZZ)
   *         "hostname": string,         // the hostname of the site where the reCAPTCHA was solved
   *         "error-codes": [...]        // optional
   *         }
   */
  def siteVerify(captchaValue: String, recaptchaSecret: String): Try[Recaptcha] =
    //POST to googleVerifyURL using query-params
    Try:
      val resp = WebClient.of()
        .post("https://www.google.com/recaptcha/api/siteverify", QueryParams.of("secret", recaptchaSecret, "response", captchaValue), "")
        .aggregate()
        .join()

      /*
       * Json format - success, challenge_ts, hostname, score, action
       */
      logger.debug(s"status:${resp.status()}, content-type:${resp.contentType()}, content:${resp.contentUtf8()}")
      assert(resp.contentType().belongsTo(MediaType.JSON))
      new Recaptcha(Json.parse(resp.contentUtf8()))


case class Recaptcha(jsValue: JsValue):
  def success:Boolean =
    (jsValue \ "success").asOpt[Boolean]
      .getOrElse(false)

  def fail: Boolean = !success

  def scoreOpt:Option[Double] =
    (jsValue \ "score").asOpt[Double]

  def score: Double = scoreOpt.getOrElse(if success then 1 else 0)

  def actionOpt: Option[String] =
    (jsValue \ "action").asOpt[String]

  def challengeTS: Option[Instant] =
    (jsValue \ "challenge_ts").asOpt[String]
      .flatMap(ts => Try(Instant.parse(ts)).toOption)

  def hostname: String =
    (jsValue \ "hostname").asOpt[String].getOrElse("")

  def errorCodes: List[String] =
    (jsValue \ "error-codes").asOpt[List[String]]
      .getOrElse(Nil)
