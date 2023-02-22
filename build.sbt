val scala3Version = "3.2.2"

scalacOptions ++= Seq("-feature", "-deprecation")

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.6.5",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "commons-i18n" % "1.0.1",
      "com.greenfossil" %% "typesafe-config-ext" % "1.0.0",
      "com.greenfossil" %% "data-mapping" % "1.0.2",
      "io.projectreactor" % "reactor-core" % "3.5.2",
      "com.linecorp.armeria" %% "armeria-scala" % "1.22.1",
      "com.linecorp.armeria" % "armeria-logback" % "1.22.1",
      "org.overviewproject" % "mime-types" % "1.0.4",
      "org.slf4j" % "slf4j-api" % "2.0.5",
      "ch.qos.logback" % "logback-classic" % "1.4.5" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )

parallelExecution := false
