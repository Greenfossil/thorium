package com.greenfossil.webserver

import scala.concurrent.duration.FiniteDuration

class HttpConfiguration {

}

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
                                 cookieName: String = "PLAY_SESSION",
                                 secure: Boolean = false,
                                 maxAge: Option[FiniteDuration] = None, //Expiry?
                                 httpOnly: Boolean = true,
                                 domain: Option[String] = None,
                                 path: String = "/",
//                                 sameSite: Option[SameSite] = Some(SameSite.Lax),
//                                 jwt: JWTConfiguration = JWTConfiguration()
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
                               cookieName: String = "PLAY_FLASH",
                               secure: Boolean = false,
                               httpOnly: Boolean = true,
                               domain: Option[String] = None,
                               path: String = "/",
//                               sameSite: Option[SameSite] = Some(SameSite.Lax),
//                               jwt: JWTConfiguration = JWTConfiguration()
                             )

/**
 * The application secret. Must be set. A value of "changeme" will cause the application to fail to start in
 * production.
 *
 * With the Play secret we want to:
 *
 * 1. Encourage the practice of *not* using the same secret in dev and prod.
 * 2. Make it obvious that the secret should be changed.
 * 3. Ensure that in dev mode, the secret stays stable across restarts.
 * 4. Ensure that in dev mode, sessions do not interfere with other applications that may be or have been running
 *   on localhost.  Eg, if I start Play app 1, and it stores a PLAY_SESSION cookie for localhost:9000, then I stop
 *   it, and start Play app 2, when it reads the PLAY_SESSION cookie for localhost:9000, it should not see the
 *   session set by Play app 1.  This can be achieved by using different secrets for the two, since if they are
 *   different, they will simply ignore the session cookie set by the other.
 *
 * To achieve 1 and 2, we will, in Activator templates, set the default secret to be "changeme".  This should make
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
 * Play secret is checked for a minimum length in production:
 *
 * 1. If the key is fifteen characters or fewer, a warning will be logged.
 * 2. If the key is eight characters or fewer, then an error is thrown and the configuration is invalid.
 *
 * @param secret   the application secret
 * @param provider the JCE provider to use. If null, uses the platform default
 */
case class SecretConfiguration(secret: String = "changeme", provider: Option[String] = None)

/**
 * The JSON Web Token configuration
 *
 * @param signatureAlgorithm The signature algorithm used to sign the JWT
 * @param expiresAfter The period of time after which the JWT expires, if any.
 * @param clockSkew The amount of clock skew to permit for expiration / not before checks
 * @param dataClaim The claim key corresponding to the data map passed in by the user
 */
import concurrent.duration.*
case class JWTConfiguration(
                             signatureAlgorithm: String = "HS256",
                             expiresAfter: Option[FiniteDuration] = None,
                             clockSkew: FiniteDuration = 30.seconds,
                             dataClaim: String = "data"
                           )

object JWTConfigurationParser {
  def apply(config: Configuration, parent: String): JWTConfiguration = {
//    JWTConfiguration(
//      signatureAlgorithm = config.get[String](s"${parent}.signatureAlgorithm"),
//      expiresAfter = config.get[Option[FiniteDuration]](s"${parent}.expiresAfter"),
//      clockSkew = config.get[FiniteDuration](s"${parent}.clockSkew"),
//      dataClaim = config.get[String](s"${parent}.dataClaim")
//    )
    ???
  }
}

case class Configuration(config: String) //Typesafe config