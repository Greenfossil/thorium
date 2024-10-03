val scala3Version = "3.5.0"

scalacOptions ++= Seq("-feature", "-deprecation", "-Wunused:all" )

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.8.0",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "htmltags" % "1.1.0",
      "com.greenfossil" %% "data-mapping" % "1.1.0",
      "com.greenfossil" %% "commons-i18n" % "1.1.0",
      "com.greenfossil" %% "typesafe-config-ext" % "1.1.0",
      "io.projectreactor" % "reactor-core" % "3.6.8",
      "com.linecorp.armeria" % "armeria" % "1.30.1",
      "com.linecorp.armeria" % "armeria-logback" % "1.30.1",
      "org.overviewproject" % "mime-types" % "2.0.0",
      "io.github.yskszk63" % "jnhttp-multipartformdata-bodypublisher" % "0.0.1",
      "org.slf4j" % "slf4j-api" % "2.0.12",
      "com.microsoft.playwright" % "playwright" % "1.45.1" % Test,
      "ch.qos.logback" % "logback-classic" % "1.5.6" % Test,
      "org.scalameta" %% "munit" % "1.0.0" % Test
    )
  )

//This is required for testcases that submits header content-length explicitly
javacOptions += "-Djdk.httpclient.allowRestrictedHeaders=content-length"

//https://www.scala-sbt.org/1.x/docs/Publishing.html
ThisBuild / versionScheme := Some("early-semver")
