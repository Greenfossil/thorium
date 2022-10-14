package com.greenfossil.thorium

import com.typesafe.config.{Config, ConfigFactory}

object Configuration:

  def apply(): Configuration = from(getClass.getClassLoader)

  def usingPort(port: Int): Configuration =
    val configuration = from(getClass.getClassLoader)
    configuration.copy(httpConfiguration = configuration.httpConfiguration.copy(httpPort = port))

  def from(classLoader: ClassLoader): Configuration =
    from(ConfigFactory.load(classLoader))

  def from(config: Config): Configuration =
    val env = Environment.from(config)
    new Configuration(config, env, HttpConfiguration.from(config, env))

case class Configuration(config: Config, environment: Environment, httpConfiguration: HttpConfiguration) :
  def httpPort: Int = httpConfiguration.httpPort

  def maxRequestLength = httpConfiguration.maxRequestLength

  def maxNumConnectionOpt = httpConfiguration.maxNumConnectionOpt

  def requestTimeout = httpConfiguration.requestTimeout

  def isProd: Boolean = environment.isProd

  def isDev: Boolean = environment.isDev

  def isDemo: Boolean = environment.isDemo

  def isTest: Boolean = environment.isTest



