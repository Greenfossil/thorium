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

import java.time.Instant
import scala.annotation.targetName

inline val SessionID = "sessionID"

object Session:
  def apply(): Session =
    Session(Map.empty.withDefaultValue(""), Instant.now)

  def apply(data: Map[String, String]): Session =
    Session(data.withDefaultValue(""), Instant.now)


/**
 * 
 * HTTP Session.
 * Session data are encoded into an HTTP cookie, and can only contain simple String values.
 * @param data
 */
case class Session(data: Map[String, String], created: Instant):
  export data.{- as _, apply as  _, *}

  def idOpt: Option[String] = data.get(SessionID)

  def apply(key: String): String = data(key)

  /**
   * Returns a new session with the given key-value pair added.
   *
   * For example:
   * {{{
   * session + ("username" -> "bob")
   * }}}
   *
   * @param kv the key-value pair to add
   * @return the modified session
   */
  @targetName("add")
  def +(kv: (String, String)): Session = 
    require(kv._2 != null, s"Session value for ${kv._1} cannot be null")
    copy(data + kv)
  
  @targetName("add")
  def +(name: String, value: String): Session =
    this.+((name, value))

  @targetName("add")
  def +(newSession: Session): Session =
    copy(data = data ++ newSession.data)
  
  @targetName("add")
  def +(newSession: Map[String, String]): Session =
    copy(data = data ++ newSession)

  /**
   * Returns a new session with elements added from the given `Iterable`.
   *
   * @param kvs an `Iterable` containing key-value pairs to add.
   */
  @targetName("concat")
  def ++(kvs: Iterable[(String, String)]): Session = 
    for ((k, v) <- kvs) require(v != null, s"Session value for $k cannot be null")
    copy(data ++ kvs)

  /**
   * Returns a new session with the given key removed.
   *
   * For example:
   * {{{
   * session - "username"
   * }}}
   *
   * @param key the key to remove
   * @return the modified session
   */
  @targetName("minus")
  def -(key: String): Session = copy(data - key)

  /**
   * Returns a new session with the given keys removed.
   *
   * For example:
   * {{{
   * session -- Seq("username", "name")
   * }}}
   *
   * @param keys the keys to remove
   * @return the modified session
   */
  @targetName("diff")
  def --(keys: Iterable[String]): Session = copy(data -- keys)