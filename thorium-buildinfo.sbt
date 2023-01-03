Compile / sourceGenerators  += Def.task {
  val file = ( Compile / sourceManaged ).value  / "com" / "greenfossil" / "thorium" / "ThoriumBuildInfo.scala"
  IO.write (
    file,
    s"""package com.greenfossil.thorium
       |private [thorium] object ThoriumBuildInfo {
       |  val version = "${version.value}"
       |}
       |""".stripMargin
  )
  Seq(file)
}.taskValue