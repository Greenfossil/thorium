val scala3Version = "3.3.0"

scalacOptions ++= Seq("-feature", "-deprecation", "-Wunused:all" )

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.7.3-RC1",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "htmltags" % "1.0.2",
      "com.greenfossil" %% "data-mapping" % "1.0.6",
      "com.greenfossil" %% "commons-i18n" % "1.0.2",
      "com.greenfossil" %% "typesafe-config-ext" % "1.0.1",
      "io.projectreactor" % "reactor-core" % "3.5.9",
      "com.linecorp.armeria" %% "armeria-scala" % "1.25.2",
      "com.linecorp.armeria" % "armeria-logback" % "1.25.2",
      "org.overviewproject" % "mime-types" % "1.0.4",
      "org.slf4j" % "slf4j-api" % "2.0.5",
      "ch.qos.logback" % "logback-classic" % "1.4.7" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )

parallelExecution := false
