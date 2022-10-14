package com.greenfossil.webserver

import scala.annotation.targetName

/**
 * TODO - generate an Id or nounce???
 * HTTP Session.
 * Session data are encoded into an HTTP cookie, and can only contain simple String values.
 * @param data
 */
case class Session(data: Map[String, String] = Map.empty.withDefaultValue("")):
  export data.{- as _, apply as  _, *}

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