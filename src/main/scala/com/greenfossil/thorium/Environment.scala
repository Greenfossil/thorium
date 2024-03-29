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

import com.typesafe.config.{Config, ConfigFactory}

import java.io.{File, InputStream}

enum Mode extends Enum[Mode]:
  case Dev, Test, Prod, Demo

object Environment:

  def from(classLoader: ClassLoader): Environment =
    from(ConfigFactory.load(classLoader))

  /**
   * A simple environment.
   *
   * Uses the same classloader that the environment classloader is defined in, and the current working directory as the
   * path.
   */
  def from(config: Config): Environment =
    val mode = config.getEnum(classOf[Mode], "app.env")
    Environment(config.getClass.getClassLoader, new File("."), mode)


case class Environment(classLoader: ClassLoader, rootPath: File,  mode: Mode):

  /**
   * Retrieves a file relative to the application root path.
   *
   * Note that it is up to you to manage the files in the application root path in production.  By default, there will
   * be nothing available in the application root path.
   *
   * For example, to retrieve some deployment specific data file:
   * {{{
   * val myDataFile = application.getFile("data/data.xml")
   * }}}
   *
   * @param relativePath relative path of the file to fetch
   * @return a file instance; it is not guaranteed that the file exists
   */
  def getFile(relativePath: String): File = new File(rootPath, relativePath)

  /**
   * Retrieves a file relative to the application root path.
   * This method returns an Option[File], using None if the file was not found.
   *
   * Note that it is up to you to manage the files in the application root path in production.  By default, there will
   * be nothing available in the application root path.
   *
   * For example, to retrieve some deployment specific data file:
   * {{{
   * val myDataFile = application.getExistingFile("data/data.xml")
   * }}}
   *
   * @param relativePath the relative path of the file to fetch
   * @return an existing file
   */
  def getExistingFile(relativePath: String): Option[File] = Some(getFile(relativePath)).filter(_.exists)

  /**
   * Scans the application classloader to retrieve a resource.
   *
   * The conf directory is included on the classpath, so this may be used to look up resources, relative to the conf
   * directory.
   *
   * For example, to retrieve the conf/logback.xml configuration file:
   * {{{
   * val maybeConf = application.resource("logback.xml")
   * }}}
   *
   * @param name the absolute name of the resource (from the classpath root)
   * @return the resource URL, if found
   */
  def resource(name: String): Option[java.net.URL] =
    val n = name.stripPrefix("/")
    Option(classLoader.getResource(n))

  /**
   * Scans the application classloader to retrieve a resource’s contents as a stream.
   *
   * The conf directory is included on the classpath, so this may be used to look up resources, relative to the conf
   * directory.
   *
   * For example, to retrieve the conf/logback.xml configuration file:
   * {{{
   * val maybeConf = application.resourceAsStream("logback.xml")
   * }}}
   *
   * @param name the absolute name of the resource (from the classpath root)
   * @return a stream, if found
   */
  def resourceAsStream(name: String): Option[InputStream] =
    val n = name.stripPrefix("/")
    Option(classLoader.getResourceAsStream(n))

  def isProd: Boolean = mode == Mode.Prod

  def isTest: Boolean = mode == Mode.Test

  def isDemo: Boolean = mode == Mode.Demo

  def isDev: Boolean = mode == Mode.Dev

  def modeName: String = mode.toString.toLowerCase

