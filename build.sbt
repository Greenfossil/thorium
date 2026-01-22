val scala3Version = "3.7.1"

scalacOptions ++= Seq(
//  "-feature", "-deprecation", "-Wunused:all" ,
//  "-Xprint:postInlining", "-Xmax-inlines:100000",
)

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.10.9",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "htmltags" % "1.3.1",
      "com.greenfossil" %% "data-mapping" % "1.3.7",
      "com.greenfossil" %% "commons-i18n" % "1.3.0",
      "com.greenfossil" %% "typesafe-config-ext" % "1.3.0",
      "io.projectreactor" % "reactor-core" % "3.8.1",
      "com.linecorp.armeria" % "armeria" % "1.34.2",
      "com.linecorp.armeria" % "armeria-logback" % "1.34.2",
      "org.overviewproject" % "mime-types" % "2.0.0",
      "io.github.yskszk63" % "jnhttp-multipartformdata-bodypublisher" % "0.0.1",
      "org.slf4j" % "slf4j-api" % "2.0.17",
      "com.microsoft.playwright" % "playwright" % "1.57.0" % Test,
      "ch.qos.logback" % "logback-classic" % "1.5.22" % Test,
      "org.scalameta" %% "munit" % "1.2.1" % Test,
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % Test

    )
  )

//This is required for testcases that submits header content-length explicitly
javacOptions += "-Djdk.httpclient.allowRestrictedHeaders=content-length"

//https://www.scala-sbt.org/1.x/docs/Publishing.html
ThisBuild / versionScheme := Some("early-semver")
