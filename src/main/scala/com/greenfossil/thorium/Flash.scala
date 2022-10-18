package com.greenfossil.thorium

import scala.annotation.targetName

object Flash:
  def apply(): Flash = new Flash(Map.empty.withDefaultValue(""))

case class Flash(data: Map[String, String]):
  export data.{+ as _, *}
  
  @targetName("add")
  def +(tup: (String, String)): Flash =
    copy(data =  data + tup )