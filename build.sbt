val scala3Version = "3.2.0"

scalacOptions ++= Seq("-feature", "-deprecation")

lazy val datamappingVersion="0.2.0-RC11-SNAPSHOT"
lazy val commonsJsonVersion = "0.2.0-RC1"
lazy val commonsI18nVersion = "0.2.0-RC4"
lazy val typesafeConfigExtVersion = "0.2.0-RC1"

//lazy val databMapping = RootProject(file("../data-mapping"))
//lazy val commonsI18n = RootProject(file("../commons-i18n"))

lazy val thorium = project
  .in(file("."))
  .settings(
    name := "thorium",
    organization := "com.greenfossil",
    version := "0.2.0",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "commons-i18n" % commonsI18nVersion,
      "com.greenfossil" %% "commons-json" % commonsJsonVersion,
      "com.greenfossil" %% "typesafe-config-ext" % typesafeConfigExtVersion,
      "com.greenfossil" %% "data-mapping" % datamappingVersion,
      "io.projectreactor" % "reactor-core" % "3.4.22",
      "com.linecorp.armeria" %% "armeria-scala" % "1.20.1",
      "com.linecorp.armeria" % "armeria-logback" % "1.20.1",
      "org.slf4j" % "slf4j-api" % "2.0.3",
      "ch.qos.logback" % "logback-classic" % "1.4.3" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
//  .dependsOn(databMapping)
//  .dependsOn(commonsI18n)

parallelExecution := false
