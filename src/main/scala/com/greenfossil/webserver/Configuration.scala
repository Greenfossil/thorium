package com.greenfossil.webserver

import com.typesafe.config.{Config, ConfigFactory}

object Configuration:

  def apply(): Configuration = from(getClass.getClassLoader)

  def usingPort(port: Int): Configuration =
    val config = ConfigFactory.load(getClass.getClassLoader)
    val environment = Environment.from(getClass.getClassLoader)
    new Configuration(config, environment, HttpConfiguration.from(config, environment).copy(httpPort = port))

  def from(classLoader: ClassLoader): Configuration =
    from(ConfigFactory.load(classLoader),Environment.from(classLoader))

  def from(config: Config, environment: Environment): Configuration =
    new Configuration(config, environment, HttpConfiguration.from(config, environment))




end Configuration

case class Configuration(config: Config, environment: Environment, httpConfiguration: HttpConfiguration) :
  def httpPort: Int = httpConfiguration.httpPort

  def maxRequestLength = httpConfiguration.maxRequestLength

  def maxNumConnectionOpt = httpConfiguration.maxNumConnectionOpt

  def requestTimeout = httpConfiguration.requestTimeout

  def isProd: Boolean = environment.isProd

  def isDev: Boolean = environment.isDev

  def isDemo: Boolean = environment.isDemo

  def isTest: Boolean = environment.isTest



