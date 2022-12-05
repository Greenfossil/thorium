val scala3Version = "3.2.0"

scalacOptions ++= Seq("-feature", "-deprecation")

lazy val datamappingVersion="1.0.0"
lazy val commonsJsonVersion = "1.0.0"
lazy val commonsI18nVersion = "1.0.0"
lazy val typesafeConfigExtVersion = "1.0.0"

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.5.0-SNAPSHOT",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "commons-i18n" % commonsI18nVersion,
      "com.greenfossil" %% "commons-json" % commonsJsonVersion,
      "com.greenfossil" %% "typesafe-config-ext" % typesafeConfigExtVersion,
      "com.greenfossil" %% "data-mapping" % datamappingVersion,
      "io.projectreactor" % "reactor-core" % "3.4.24",
      "com.linecorp.armeria" %% "armeria-scala" % "1.20.3",
      "com.linecorp.armeria" % "armeria-logback" % "1.20.3",
      "org.overviewproject" % "mime-types" % "1.0.4",
      "org.slf4j" % "slf4j-api" % "2.0.3",
      "ch.qos.logback" % "logback-classic" % "1.4.4" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )

parallelExecution := false
