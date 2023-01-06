val scala3Version = "3.2.0"

scalacOptions ++= Seq("-feature", "-deprecation")

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.6.4",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "commons-i18n" % "1.0.0",
      "com.greenfossil" %% "typesafe-config-ext" % "1.0.0",
      "com.greenfossil" %% "data-mapping" % "1.0.0",
      "io.projectreactor" % "reactor-core" % "3.4.24",
      "com.linecorp.armeria" %% "armeria-scala" % "1.21.0",
      "com.linecorp.armeria" % "armeria-logback" % "1.21.0",
      "org.overviewproject" % "mime-types" % "1.0.4",
      "org.slf4j" % "slf4j-api" % "2.0.3",
      "ch.qos.logback" % "logback-classic" % "1.4.4" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )

parallelExecution := false
