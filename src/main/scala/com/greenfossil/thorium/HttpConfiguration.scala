/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium

import com.typesafe.config.*
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time
import java.time.Duration
import java.util.Base64
import scala.jdk.CollectionConverters.*
import scala.util.*

type SameSiteCookie = "Strict" | "Lax" | "None"

private[thorium] val configurationLogger: Logger = LoggerFactory.getLogger("com.greenfossil.thorium.configuration")

val APP_HTTP__CONTEXT = "app.http.context"
val APP_HTTP__PORT = "app.http.port"
val APP_HTTP__MAX_NUM_CONNECTION = "app.http.maxNumConnection"
val APP_HTTP__MAX_REQUEST_LENGHT = "app.http.maxRequestLength"
val APP_HTTP__REQUEST_TIMEOUT = "app.http.requestTimeout"
val APP_HTTP__SECRET_KEY = "app.http.secret.key"
val APP_HTTP__SECRET_PROVIDER = "app.http.secret.provider"

val APP_HTTP_COOKIES__SECURE = "app.http.cookies.secure"
val APP_HTTP_COOKIES__MAX_AGE = "app.http.cookies.maxAge"
val APP_HTTP_COOKIES__HTTP_ONLY = "app.http.cookies.httpOnly"
val APP_HTTP_COOKIES__DOMAIN = "app.http.cookies.domain"
val APP_HTTP_COOKIES__PATH = "app.http.cookies.path"
val APP_HTTP_COOKIES__SAMESITE = "app.http.cookies.sameSite"
val APP_HTTP_COOKIES__HOST_ONLY = "app.http.cookies.hostOnly"
val APP_HTTP_COOKIES__JWT = "app.http.cookies.jwt"

val APP_HTTP_SESSION__COOKIE_NAME = "app.http.session.cookieName"
val APP_HTTP_SESSION__SECURE = "app.http.session.secure"
val APP_HTTP_SESSION__MAX_AGE = "app.http.session.maxAge"
val APP_HTTP_SESSION__HTTP_ONLY = "app.http.session.httpOnly"
val APP_HTTP_SESSION__DOMAIN = "app.http.session.domain"
val APP_HTTP_SESSION__PATH = "app.http.session.path"
val APP_HTTP_SESSION__SAME_SITE = "app.http.session.sameSite"
val APP_HTTP_SESSION__JWT = "app.http.session.jwt"

val APP_HTTP_FLASH__COOKIE_NAME = "app.http.flash.cookieName"
val APP_HTTP_FLASH__SECURE = "app.http.flash.secure"
val APP_HTTP_FLASH__HTTP_ONLY = "app.http.flash.httpOnly"
val APP_HTTP_FLASH__DOMAIN = "app.http.flash.domain"
val APP_HTTP_FLASH__PATH = "app.http.flash.path"
val APP_HTTP_FLASH__SAME_SITE = "app.http.flash.sameSite"
val APP_HTTP_FLASH__JWT = "app.http.flash.jwt"

val APP_HTTP_CSRF__COOKIE_NAME = "app.http.csrf.cookieName"
val APP_HTTP_CSRF__SECURE = "app.http.csrf.secure"
val APP_HTTP_CSRF__HTTP_ONLY = "app.http.csrf.httpOnly"
val APP_HTTP_CSRF__DOMAIN = "app.http.csrf.domain"
val APP_HTTP_CSRF__PATH = "app.http.csrf.path"
val APP_HTTP_CSRF__SAME_SITE = "app.http.csrf.sameSite"
val APP_HTTP_CSRF__JWT = "app.http.csrf.jwt"
val APP_HTTP_CSRF__ALLOW_PATH_PREFIXES = "app.http.csrf.allowPathPrefixes"

val APP_HTTP_RECAPTCHA__SECRETKEY = "app.http.recaptcha.secretKey"
val APP_HTTP_RECAPTCHA__SITEKEY = "app.http.recaptcha.siteKey"
val APP_HTTP_RECAPTCHA__TOKENNAME = "app.http.recaptcha.tokenName"
val APP_HTTP_RECAPTCHA__TIMEOUT = "app.http.recaptcha.timeout"

object HttpConfiguration:
  def from(config: Config, environment: Environment): HttpConfiguration = 
    HttpConfiguration(
      context = config.getString(APP_HTTP__CONTEXT),
      httpPort = config.getInt(APP_HTTP__PORT),
      maxNumConnectionOpt = config.getIntOpt(APP_HTTP__MAX_NUM_CONNECTION),
      maxRequestLength = config.getInt(APP_HTTP__MAX_REQUEST_LENGHT),
      requestTimeout = config.getDuration(APP_HTTP__REQUEST_TIMEOUT),
      cookieConfig = CookieConfiguration(
        secure = config.getBoolean(APP_HTTP_COOKIES__SECURE),
        maxAge = config.getDurationOpt(APP_HTTP_COOKIES__MAX_AGE),
        httpOnly = config.getBoolean(APP_HTTP_COOKIES__HTTP_ONLY),
        domain = config.getStringOpt(APP_HTTP_COOKIES__DOMAIN),
        path = config.getString(APP_HTTP_COOKIES__PATH),
        sameSite = config.getStringOpt(APP_HTTP_COOKIES__SAMESITE).map(_.asInstanceOf[SameSiteCookie]),
        hostOnly = config.getBooleanOpt(APP_HTTP_COOKIES__HOST_ONLY),
        jwt = JWTConfigurationParser(config, APP_HTTP_COOKIES__JWT)
      ),
      sessionConfig = SessionConfiguration(
        cookieName = config.getString(APP_HTTP_SESSION__COOKIE_NAME),
        secure = config.getBoolean(APP_HTTP_SESSION__SECURE),
        maxAge = config.getDurationOpt(APP_HTTP_SESSION__MAX_AGE),
        httpOnly = config.getBoolean(APP_HTTP_SESSION__HTTP_ONLY),
        domain = config.getStringOpt(APP_HTTP_SESSION__DOMAIN),
        path = config.getString(APP_HTTP_SESSION__PATH),
        sameSite = config.getStringOpt(APP_HTTP_SESSION__SAME_SITE).map(_.asInstanceOf[SameSiteCookie]),
        jwt = JWTConfigurationParser(config, APP_HTTP_SESSION__JWT)
      ),
      flashConfig = FlashConfiguration(
        cookieName = config.getString(APP_HTTP_FLASH__COOKIE_NAME),
        secure = config.getBoolean(APP_HTTP_FLASH__SECURE),
        httpOnly = config.getBoolean(APP_HTTP_FLASH__HTTP_ONLY),
        domain = config.getStringOpt(APP_HTTP_FLASH__DOMAIN),
        path = config.getString(APP_HTTP_FLASH__PATH),
        sameSite = config.getStringOpt(APP_HTTP_FLASH__SAME_SITE).map(_.asInstanceOf[SameSiteCookie]),
        jwt = JWTConfigurationParser(config, APP_HTTP_FLASH__JWT)
      ),
      csrfConfig = CSRFConfiguration(
        cookieName = config.getString(APP_HTTP_CSRF__COOKIE_NAME),
        secure = config.getBoolean(APP_HTTP_CSRF__SECURE),
        httpOnly = config.getBoolean(APP_HTTP_CSRF__HTTP_ONLY),
        domain = config.getStringOpt(APP_HTTP_CSRF__DOMAIN),
        path = config.getString(APP_HTTP_CSRF__PATH),
        sameSite = config.getStringOpt(APP_HTTP_CSRF__SAME_SITE).map(_.asInstanceOf[SameSiteCookie]),
        jwt = JWTConfigurationParser(config, APP_HTTP_CSRF__JWT),
        allowPathPrefixes = config.getStringList(APP_HTTP_CSRF__ALLOW_PATH_PREFIXES).asScala.toSeq
      ),
      recaptchaConfig = RecaptchaConfiguration(
        secretKey = config.getString(APP_HTTP_RECAPTCHA__SECRETKEY),
        siteKey = config.getString(APP_HTTP_RECAPTCHA__SITEKEY),
        tokenName = config.getString(APP_HTTP_RECAPTCHA__TOKENNAME),
        timeout = config.getInt(APP_HTTP_RECAPTCHA__TIMEOUT)
      ),
      secretConfig = getSecretConfiguration(config, environment),
      environment = environment
    )


/**
 *
 * @param context
 * @param httpPort The HTTP port number
 * @param maxConnectionOpt The maximum number of accepted HTTP connections
 * @param maxRequestLength The maximum allowed length of HTTP content in bytes
 * @param requestTimeout Number of seconds before HTTP request times out
 * @param sessionConfig The session configuration
 * @param flashConfig The flash configuration
 * @param secretConfig The application secret
 * @param environment The application environment
 */
case class HttpConfiguration(
   context: String = "/",
   httpPort: Int = 8080,
   maxNumConnectionOpt: Option[Int] = None,
   maxRequestLength: Int = 10485760,
   requestTimeout: Duration = Duration.ofSeconds(10),
   cookieConfig: CookieConfiguration = CookieConfiguration(),
   sessionConfig: SessionConfiguration = SessionConfiguration(),
   flashConfig: FlashConfiguration = FlashConfiguration(),
   csrfConfig: CSRFConfiguration = CSRFConfiguration(),
   recaptchaConfig: RecaptchaConfiguration = RecaptchaConfiguration("change-me","change-me", "g-recaptcha-response", 3000),
   secretConfig: SecretConfiguration = SecretConfiguration(),
   environment: Environment
)

trait CookieConfigurationLike:
  val secure: Boolean
  val maxAge: Option[Duration]
  val httpOnly: Boolean
  val domain: Option[String]
  val path: String
  val sameSite: Option[SameSiteCookie]
  val hostOnly: Option[Boolean]
  val jwt: JWTConfiguration


/**
 * The cookie configuration
 *
 * @param secure     Whether the session cookie should set the secure flag or not
 * @param maxAge     The max age of the session, none, use "session" sessions
 * @param httpOnly   Whether the HTTP only attribute of the cookie should be set
 * @param domain     The domain to set for the session cookie, if defined
 * @param path       The path for which this cookie is valid
 * @param sameSite   The cookie's SameSite attribute
 * @param hostOnly   The cookie's host-only attribute
 * @param jwt        The JWT specific information
 */
case class CookieConfiguration(
   secure: Boolean = false,
   maxAge: Option[Duration] = None,
   httpOnly: Boolean = true,
   domain: Option[String] = None,
   path: String = "/",
   sameSite: Option[SameSiteCookie] = Some("Lax"),
   hostOnly: Option[Boolean] = Some(false),
   jwt: JWTConfiguration = JWTConfiguration()
)  extends CookieConfigurationLike

/**
 * The session configuration
 *
 * @param cookieName The name of the cookie used to store the session
 * @param secure     Whether the session cookie should set the secure flag or not
 * @param maxAge     The max age of the session, none, use "session" sessions
 * @param httpOnly   Whether the HTTP only attribute of the cookie should be set
 * @param domain     The domain to set for the session cookie, if defined
 * @param path       The path for which this cookie is valid
 * @param sameSite   The cookie's SameSite attribute
 * @param jwt        The JWT specific information
 */
case class SessionConfiguration(
   cookieName: String = "APP_SESSION",
   secure: Boolean = false,
   maxAge: Option[Duration] = None,
   httpOnly: Boolean = true,
   domain: Option[String] = None,
   path: String = "/",
   sameSite: Option[SameSiteCookie] = Some("Lax"),
   hostOnly: Option[Boolean] = None,
   jwt: JWTConfiguration = JWTConfiguration()
) extends CookieConfigurationLike

/**
 * The flash configuration
 *
 * @param cookieName The name of the cookie used to store the session
 * @param secure     Whether the flash cookie should set the secure flag or not
 * @param httpOnly   Whether the HTTP only attribute of the cookie should be set
 * @param domain     The domain to set for the session cookie, if defined
 * @param path       The path for which this cookie is valid
 * @param sameSite   The cookie's SameSite attribute
 * @param jwt        The JWT specific information
 */
case class FlashConfiguration(
   cookieName: String = "APP_FLASH",
   secure: Boolean = false,
   maxAge: Option[Duration] = None,
   httpOnly: Boolean = true,
   domain: Option[String] = None,
   path: String = "/",
   sameSite: Option[SameSiteCookie] = Some("Lax"),
   hostOnly: Option[Boolean] = None,
   jwt: JWTConfiguration = JWTConfiguration()
) extends CookieConfigurationLike

/**
 * The csrf configuration
 *
 * @param cookieName The name of the cookie used to store the session
 * @param secure     Whether the csrf cookie should set the secure flag or not
 * @param httpOnly   Whether the HTTP only attribute of the cookie should be set
 * @param domain     The domain to set for the session cookie, if defined
 * @param path       The path for which this cookie is valid
 * @param sameSite   The cookie's SameSite attribute
 * @param jwt        The JWT specific information
 */
case class CSRFConfiguration(
                              cookieName: String = "APP_CSRF_TOKEN",
                              secure: Boolean = false,
                              maxAge: Option[Duration] = None,
                              httpOnly: Boolean = true,
                              domain: Option[String] = None,
                              path: String = "/",
                              sameSite: Option[SameSiteCookie] = Some("Strict"),
                              hostOnly: Option[Boolean] = None,
                              jwt: JWTConfiguration = JWTConfiguration(),
                              allowPathPrefixes: Seq[String] = Nil
) extends CookieConfigurationLike

case class RecaptchaConfiguration(
                                 secretKey: String,
                                 siteKey: String,
                                 tokenName: String,
                                 timeout: Int /*request timeout*/,
                                 )

/**
 * The application secret. Must be set. A value of "changeme" will cause the application to fail to start in
 * production.
 *
 * 1. Encourage the practice of *not* using the same secret in dev and prod.
 * 2. Make it obvious that the secret should be changed.
 * 3. Ensure that in dev mode, the secret stays stable across restarts.
 * 4. Ensure that in dev mode, sessions do not interfere with other applications that may be or have been running
 *   on localhost.  Eg, if start App 1, and it stores a APP_SESSION cookie for localhost:8080, then stop
 *   it, and start App 2, when it reads the APP_SESSION cookie for localhost:8080, it should not see the
 *   session set by App 1.  This can be achieved by using different secrets for the two, since if they are
 *   different, they will simply ignore the session cookie set by the other.
 *
 * To achieve 1 and 2, set the default secret to be "changeme".  This should make
 * it obvious that the secret needs to be changed and discourage using the same secret in dev and prod.
 *
 * For safety, if the secret is not set, or if it's changeme, and we are in prod mode, then we will fail fatally.
 * This will further enforce both 1 and 2.
 *
 * To achieve 3, if in dev or test mode, if the secret is either changeme or not set, we will generate a secret
 * based on the location of application.conf.  This should be stable across restarts for a given application.
 *
 * To achieve 4, using the location of application.conf to generate the secret should ensure this.
 *
 * App secret is checked for a minimum length in production:
 *
 * 1. If the key is fifteen characters or fewer, a warning will be logged.
 * 2. If the key is eight characters or fewer, then an error is thrown and the configuration is invalid.
 *
 * @param secret   the application secret
 * @param provider the JCE provider to use. If null, uses the platform default
 */
case class SecretConfiguration(secret: String = "changeme", provider: Option[String] = None)

object SecretConfiguration:

  // https://crypto.stackexchange.com/a/34866 = 32 bytes (256 bits)
  // https://security.stackexchange.com/a/11224 = (128 bits is more than enough)
  // but if we have less than 8 bytes in production then it's not even 64 bits.
  // which is almost certainly not from base64'ed /dev/urandom in any case, and is most
  // probably a hardcoded text password.
  // https://tools.ietf.org/html/rfc2898#section-4.1
  val SHORTEST_SECRET_LENGTH = 9

  // https://crypto.stackexchange.com/a/34866 = 32 bytes (256 bits)
  // https://security.stackexchange.com/a/11224 = (128 bits is more than enough)
  // 86 bits of random input is enough for a secret.  This rounds up to 11 bytes.
  // If we assume base64 encoded input, this comes out to at least 15 bytes, but
  // it's highly likely to be a user inputted string, which has much, much lower
  // entropy.
  val SHORT_SECRET_LENGTH = 16

/**
 * The JSON Web Token configuration
 *
 * @param signatureAlgorithm The signature algorithm used to sign the JWT
 * @param expiresAfter The period of time after which the JWT expires, if any.
 * @param clockSkew The amount of clock skew to permit for expiration / not before checks
 * @param dataClaim The claim key corresponding to the data map passed in by the user
 */
case class JWTConfiguration(
                             signatureAlgorithm: String = "HS256",
                             expiresAfter: Option[Duration] = None,
                             clockSkew: Duration = Duration.ofSeconds(30),
                             dataClaim: String = "data"
                           )

object JWTConfigurationParser:
  def apply(config: Config, parent: String): JWTConfiguration =
    JWTConfiguration(
      signatureAlgorithm = config.getString(s"${parent}.signatureAlgorithm"),
      expiresAfter = config.getDurationOpt(s"${parent}.expiresAfter"),
      clockSkew = config.getDuration(s"${parent}.clockSkew"),
      dataClaim = config.getString(s"${parent}.dataClaim")
    )

private def configError(
  message: String,
  origin: Option[ConfigOrigin] = None,
  e: Option[Throwable] = None
): ExceptionSource = {
  /*
    The stable values here help us from putting a reference to a ConfigOrigin inside the anonymous ExceptionSource.
    This is necessary to keep the Exception serializable, because ConfigOrigin is not serializable.
   */
  val originLine       = origin.map(_.lineNumber: java.lang.Integer).orNull
  val originSourceName = origin.map(_.filename).orNull
  val originUrlOpt     = origin.flatMap(o => Option(o.url))

  def readUrlAsString(url: java.net.URL): String =
    Using.resource(url.openStream()){is =>
      val buffer = Array.ofDim[Byte](is.available())
      is.read(buffer)
      new String(buffer)
    }

  new ExceptionSource:
    val title = "Configuration error"
    val description = message
    val cause = e.orNull
    val id = nextId
    def line              = originLine
    def position          = null
    def input             = originUrlOpt.map(readUrlAsString).orNull
    def sourceName        = originSourceName
    override def toString = "Configuration error: " + message
}

/**
 * Computes the MD5 digest for a byte array.
 *
 * @param bytes the data to hash
 * @return the MD5 digest, encoded as a hex string
 */
private def md5(text: String): String =
  val digest = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8))
  new String(Base64.getEncoder.encode(digest))

private def getSecretConfiguration(config: Config, environment: Environment): SecretConfiguration =
  /**
   * Creates a configuration error for a specific configuration key.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
   * }}}
   *
   * @param path    the configuration key, related to this error
   * @param message the error message
   * @param e       the related exception
   * @return a configuration exception
   */
  def reportError(path: String, message: String, e: Option[Throwable] = None) =
    val origin = Option(if (config.hasPath(path)) config.getValue(path).origin else config.root.origin)
    configError(message, origin, e)

  val Blank = """\s*""".r

  val secret =
    Option(config.getString(APP_HTTP__SECRET_KEY)) match {
      case Some("changeme") | Some(Blank()) | None if environment.isProd =>
        val message =
          """
            |The application secret has not been set, and we are in prod mode. Your application is not secure.
          """.stripMargin
        throw reportError("app.http.secret", message)

      case Some(s) if s.length < SecretConfiguration.SHORTEST_SECRET_LENGTH && environment.isProd =>
        val message =
          """
            |The application secret is too short and does not have the recommended amount of entropy.  Your application is not secure.
          """.stripMargin
        throw reportError("app.http.secret", message)

      case Some(s) if s.length < SecretConfiguration.SHORT_SECRET_LENGTH && environment.isProd =>
        val message =
          """
            |Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure.
            |The application secret should ideally be 32 bytes of completely random input, encoded in base64.
          """.stripMargin
        configurationLogger.warn(message)
        s

      case Some(s)
        if s.length < SecretConfiguration.SHORTEST_SECRET_LENGTH && !s.equals("changeme") && s.trim.nonEmpty && environment.isDev =>
        val message =
          """
            |The application secret is too short and does not have the recommended amount of entropy.  Your application is not secure
            |and it will fail to start in production mode.
          """.stripMargin
        configurationLogger.warn(message)
        s

      case Some(s)
        if s.length < SecretConfiguration.SHORT_SECRET_LENGTH && !s.equals("changeme") && s.trim.nonEmpty && environment.isDev =>
        val message =
          """
            |Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure.
            |The application secret should ideally be 32 bytes of completely random input, encoded in base64. While the application
            |will be able to start in production mode, you will also see a warning when it is starting.
          """.stripMargin
        configurationLogger.warn(message)
        s

      case Some("changeme") | Some(Blank()) | None =>
        val appConfLocation = environment.resource("application.conf")
        // Try to generate a stable secret. Security is not the issue here, since this is just for tests and dev mode.
        val secret = appConfLocation.fold(
          // No application.conf?  Oh well, just use something hard coded.
          "she sells sea shells on the sea shore"
        )(_.toString)
        val md5Secret = md5(secret)
        configurationLogger.debug(
          s"Generated dev mode secret $md5Secret for app at ${appConfLocation.getOrElse("unknown location")}"
        )
        md5Secret
      case Some(s) => s
    }

  val provider = config.getStringOpt(APP_HTTP__SECRET_PROVIDER)
  SecretConfiguration(String.valueOf(secret), provider)
