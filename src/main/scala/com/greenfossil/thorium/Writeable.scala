package com.greenfossil.thorium

import com.linecorp.armeria.common.MediaType

import java.io.{ByteArrayInputStream, InputStream}

trait Writeable[-C] :
  def content(x: C):(MediaType, InputStream)

given Writeable[String] with
  def content(x: String) = (MediaType.PLAIN_TEXT_UTF_8, new ByteArrayInputStream(x.getBytes))

import com.greenfossil.commons.json.JsValue
given Writeable[JsValue] with
  def content(x: JsValue) = (MediaType.JSON, new ByteArrayInputStream(x.stringify.getBytes("UTF-8")))