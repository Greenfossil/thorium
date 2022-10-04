val scala3Version = "3.2.0"

scalacOptions ++= Seq("-feature", "-deprecation")

lazy val datamappingVersion="0.2.0-RC10"
lazy val commonsJsonVersion = "0.2.0-RC1"
lazy val commonsI18nVersion = "0.2.0-RC3"
lazy val commonsCryptoVersion = "0.2.0-RC2"
lazy val typesafeConfigExtVersion = "0.2.0-RC1"

//lazy val databMapping = RootProject(file("../data-mapping"))

lazy val webServer = project
  .in(file("."))
  .settings(
    name := "web-server",
    organization := "com.greenfossil",
    version := "0.2.0-RC10-SNAPSHOT",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "commons-crypto" % commonsCryptoVersion,
      "com.greenfossil" %% "commons-i18n" % commonsI18nVersion,
      "com.greenfossil" %% "commons-json" % commonsJsonVersion,
      "com.greenfossil" %% "typesafe-config-ext" % typesafeConfigExtVersion,
      "com.greenfossil" %% "data-mapping" % datamappingVersion,
      "io.projectreactor" % "reactor-core" % "3.4.22",
      "com.linecorp.armeria" %% "armeria-scala" % "1.19.0",
      "com.linecorp.armeria" % "armeria-logback" % "1.19.0",
      "org.slf4j" % "slf4j-api" % "2.0.0",
      "ch.qos.logback" % "logback-classic" % "1.4.0" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
//  .dependsOn(databMapping)

parallelExecution := false
