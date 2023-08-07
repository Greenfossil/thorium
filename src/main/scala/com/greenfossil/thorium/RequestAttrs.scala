package com.greenfossil.thorium

import java.time.ZoneId

object RequestAttrs:
  import io.netty.util.AttributeKey
  val TZ = AttributeKey.valueOf[ZoneId]("tz")
  val Session = AttributeKey.valueOf[Session]("session")
  val Flash = AttributeKey.valueOf[Flash]("flash")
  val Config = AttributeKey.valueOf[Configuration]("config")
  val Request = AttributeKey.valueOf[Request]("request")
  val CSRFToken = AttributeKey.valueOf[String]("csrf-token")