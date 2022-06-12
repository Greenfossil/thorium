package com.greenfossil.webserver

import com.typesafe.config.*
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time
import java.time.Duration
import java.util.Base64
import scala.concurrent.duration.*
import scala.util.*

type SameSiteCookie = "Strict" | "Lax"

object HttpConfiguration{

  def from(config: Config, environment: Environment): HttpConfiguration = {
    HttpConfiguration(
      context = config.getString("app.http.context"),
      httpPort = config.getInt("app.http.port"),
      maxNumConnectionOpt = config.getIntOpt("app.http.maxNumConnection"),
      maxRequestLength = config.getInt("app.http.maxRequestLength"),
      requestTimeout = config.getDuration("app.http.requestTimeout"),
      cookieConfig = CookieConfiguration(
        secure = config.getBoolean("app.http.cookies.secure"),
        maxAge = config.getDurationOpt("app.http.cookies.maxAge"),
        httpOnly = config.getBoolean("app.http.cookies.httpOnly"),
        domain = config.getStringOpt("app.http.cookies.domain"),
        path = config.getString("app.http.cookies.path"),
        sameSite = config.getStringOpt("app.http.cookies.sameSite").map(_.asInstanceOf[SameSiteCookie]),
        hostOnly = config.getBoolean("app.http.cookies.hostOnly"),
        jwt = JWTConfigurationParser(config, "app.http.cookies.jwt")
      ),
      sessionConfig = SessionConfiguration(
        cookieName = config.getString("app.http.session.cookieName"),
        secure = config.getBoolean("app.http.session.secure"),
        maxAge = config.getDurationOpt("app.http.session.maxAge"),
        httpOnly = config.getBoolean("app.http.session.httpOnly"),
        domain = config.getStringOpt("app.http.session.domain"),
        path = config.getString("app.http.session.path"),
        sameSite = config.getStringOpt("app.http.session.sameSite").map(_.asInstanceOf[SameSiteCookie]),
        jwt = JWTConfigurationParser(config, "app.http.session.jwt")
      ),
      flashConfig = FlashConfiguration(
        cookieName = config.getString("app.http.flash.cookieName"),
        secure = config.getBoolean("app.http.flash.secure"),
        httpOnly = config.getBoolean("app.http.flash.httpOnly"),
        domain = config.getStringOpt("app.http.flash.domain"),
        path = config.getString("app.http.flash.path"),
        sameSite = config.getStringOpt("app.http.flash.sameSite").map(_.asInstanceOf[SameSiteCookie]),
        jwt = JWTConfigurationParser(config, "app.http.flash.jwt")
      ),
      secretConfig = getSecretConfiguration(config, environment),
      environment = environment
    )

  }

}

/**
 *
 * @param context
 * @param httpPort The HTTP port number
 * @param maxConnection The maximum number of accepted HTTP connections
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
   secretConfig: SecretConfiguration = SecretConfiguration(),
   environment: Environment
)

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
   hostOnly: Boolean = false,
   jwt: JWTConfiguration = JWTConfiguration()
)

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
   jwt: JWTConfiguration = JWTConfiguration()
)

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
   httpOnly: Boolean = true,
   domain: Option[String] = None,
   path: String = "/",
   sameSite: Option[SameSiteCookie] = Some("Lax"),
   jwt: JWTConfiguration = JWTConfiguration()
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

object SecretConfiguration {

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

}

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

object JWTConfigurationParser {
  def apply(config: Config, parent: String): JWTConfiguration =
    JWTConfiguration(
      signatureAlgorithm = config.getString(s"${parent}.signatureAlgorithm"),
      expiresAfter = config.getDurationOpt(s"${parent}.expiresAfter"),
      clockSkew = config.getDuration(s"${parent}.clockSkew"),
      dataClaim = config.getString(s"${parent}.dataClaim")
    )
}

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
    scala.util.Using
    Using.resource(url.openStream()){is =>
      val len = is.available()
      val buffer = Array.ofDim[Byte](is.available())
      is.read(buffer)
      new String(buffer)
    }

  new ExceptionSource {
    val title = "Configuration error"
    val description = message
    val cause = e.orNull
    val id = nextId
    def line              = originLine
    def position          = null
    def input             = originUrlOpt.map(readUrlAsString).orNull
    def sourceName        = originSourceName
    override def toString = "Configuration error: " + getMessage
  }
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

private def getSecretConfiguration(config: Config, environment: Environment): SecretConfiguration = {
  val logger: Logger = LoggerFactory.getLogger("app.configuration")
  /**
   * Creates a configuration error for a specific configuration key.
   *
   * For example:
   * {{{
   * val configuration = Configuration.load()
   * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
   * }}}
   *
   * @param path the configuration key, related to this error
   * @param message the error message
   * @param e the related exception
   * @return a configuration exception
   */
  def reportError(path: String, message: String, e: Option[Throwable] = None) = {
    val origin = Option(if (config.hasPath(path)) config.getValue(path).origin else config.root.origin)
    configError(message, origin, e)
  }

  val Blank = """\s*""".r

  val secret =
    Option(config.getString("app.http.secret.key")) match {
      case Some("changeme") | Some(Blank()) | None if  environment.mode == Mode.Prod =>
        val message =
          """
            |The application secret has not been set, and we are in prod mode. Your application is not secure.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        throw reportError("app.http.secret", message)

      case Some(s) if s.length < SecretConfiguration.SHORTEST_SECRET_LENGTH && environment.mode == Mode.Prod =>
        val message =
          """
            |The application secret is too short and does not have the recommended amount of entropy.  Your application is not secure.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        throw reportError("app.http.secret", message)

      case Some(s) if s.length < SecretConfiguration.SHORT_SECRET_LENGTH && environment.mode == Mode.Prod =>
        val message =
          """
            |Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure.
            |The application secret should ideally be 32 bytes of completely random input, encoded in base64.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        logger.warn(message)
        s

      case Some(s)
        if s.length < SecretConfiguration.SHORTEST_SECRET_LENGTH && !s.equals("changeme") && s.trim.nonEmpty && environment.mode == Mode.Dev =>
        val message =
          """
            |The application secret is too short and does not have the recommended amount of entropy.  Your application is not secure
            |and it will fail to start in production mode.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        logger.warn(message)
        s

      case Some(s)
        if s.length < SecretConfiguration.SHORT_SECRET_LENGTH && !s.equals("changeme") && s.trim.nonEmpty && environment.mode == Mode.Dev =>
        val message =
          """
            |Your secret key is very short, and may be vulnerable to dictionary attacks.  Your application may not be secure.
            |The application secret should ideally be 32 bytes of completely random input, encoded in base64. While the application
            |will be able to start in production mode, you will also see a warning when it is starting.
            |To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret
          """.stripMargin
        logger.warn(message)
        s

      case Some("changeme") | Some(Blank()) | None =>
        val appConfLocation = environment.resource("application.conf")
        // Try to generate a stable secret. Security is not the issue here, since this is just for tests and dev mode.
        val secret = appConfLocation.fold(
          // No application.conf?  Oh well, just use something hard coded.
          "she sells sea shells on the sea shore"
        )(_.toString)
        val md5Secret = md5(secret)
        logger.debug(
          s"Generated dev mode secret $md5Secret for app at ${appConfLocation.getOrElse("unknown location")}"
        )
        md5Secret
      case Some(s) => s
    }

  val provider = config.getStringOpt("app.http.secret.provider")

  SecretConfiguration(String.valueOf(secret), provider)
}
