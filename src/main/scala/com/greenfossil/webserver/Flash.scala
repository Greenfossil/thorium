package com.greenfossil.webserver

object Flash {
  val empty: Flash = Flash(Map.empty)
}

case class Flash(data: Map[String, String]) {
  export data.{+ as _, *}
  def + (tup: (String, String)): Flash =
    copy(data =  data + tup )
}
