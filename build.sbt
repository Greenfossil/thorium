val scala3Version = "3.3.1"

scalacOptions ++= Seq("-feature", "-deprecation", "-Wunused:all" )

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.7.18",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "htmltags" % "1.0.4",
      "com.greenfossil" %% "data-mapping" % "1.0.13",
      "com.greenfossil" %% "commons-i18n" % "1.0.9",
      "com.greenfossil" %% "typesafe-config-ext" % "1.0.2",
      "io.projectreactor" % "reactor-core" % "3.5.9",
      "com.linecorp.armeria" % "armeria" % "1.27.1",
      "com.linecorp.armeria" % "armeria-logback" % "1.27.1",
      "org.overviewproject" % "mime-types" % "1.0.4",
      "org.slf4j" % "slf4j-api" % "2.0.10",
      "com.microsoft.playwright" % "playwright" % "1.41.2" % Test,
      "ch.qos.logback" % "logback-classic" % "1.4.14" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )

parallelExecution := false
