
scalacOptions ++= Seq(
  "-feature", "-deprecation", "-Wunused:imports" ,
//  "-Xprint:postInlining", "-Xmax-inlines:100000",
)

val scala3Version = "3.8.3"
val armeriaVersion = "1.39.0"

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.11.0",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "htmltags" % "1.4.0",
      "com.greenfossil" %% "data-mapping" % "1.4.0",
      "com.greenfossil" %% "commons-i18n" % "1.4.0",
      "com.greenfossil" %% "typesafe-config-ext" % "1.4.0",
      "io.projectreactor" % "reactor-core" % "3.8.5",
      "com.linecorp.armeria" % "armeria" % armeriaVersion,
      "com.linecorp.armeria" % "armeria-logback" % armeriaVersion,
      "org.overviewproject" % "mime-types" % "2.0.0",
      "io.github.yskszk63" % "jnhttp-multipartformdata-bodypublisher" % "0.0.1",
      "org.slf4j" % "slf4j-api" % "2.0.18",
      "com.microsoft.playwright" % "playwright" % "1.60.0" % Test,
      "ch.qos.logback" % "logback-classic" % "1.5.32" % Test,
      "org.scalameta" %% "munit" % "1.3.0" % Test,
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % Test

    )
  )

//This is required for testcases that submits header content-length explicitly
javacOptions += "-Djdk.httpclient.allowRestrictedHeaders=content-length"

//https://www.scala-sbt.org/1.x/docs/Publishing.html
ThisBuild / versionScheme := Some("early-semver")
