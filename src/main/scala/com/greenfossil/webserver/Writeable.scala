package com.greenfossil.webserver

import com.linecorp.armeria.common.MediaType

trait Writeable[C] :
  def content(x: C):(MediaType, Array[Byte])

given [C](using w: Writeable[C]): Writeable[List[C]] with
  def content(x: List[C]) = ???


given Writeable[String] with
  def content(x: String) = (MediaType.PLAIN_TEXT_UTF_8, x.getBytes)


//TODO - can subtypes be implemented using a single TypeClass ?
import com.greenfossil.commons.json.{JsObject, JsValue}
//given Writeable[JsValue] with
//  def content(x: JsValue) = (MediaType.JSON, x.toJson.getBytes)

given Writeable[JsObject] with
  def content(x: JsObject) = (MediaType.JSON, x.toJson.getBytes)
