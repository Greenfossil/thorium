package com.greenfossil.webserver.data

case class FormError(key: String, messages: Seq[String], args: Seq[Any] = Nil)