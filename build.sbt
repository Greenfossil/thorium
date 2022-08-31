val scala3Version = "3.1.3"

scalacOptions ++= Seq("-feature", "-deprecation")

lazy val commonsCryptoVersion = "0.1.0"
lazy val commonsI18nVersion = "0.1.2"
lazy val commonsJsonVersion = "0.1.1"
lazy val datamappingVersion="0.1.3"
lazy val typesafeConfigExtVersion = "0.1.0"

lazy val webServer = project
  .in(file("."))
  .settings(
    name := "web-server",
    organization := "com.greenfossil",
    version := "0.1.1",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "commons-crypto" % commonsCryptoVersion,
      "com.greenfossil" %% "commons-i18n" % commonsI18nVersion,
      "com.greenfossil" %% "commons-json" % commonsJsonVersion,
      "com.greenfossil" %% "typesafe-config-ext" % typesafeConfigExtVersion,
      "com.greenfossil" %% "data-mapping" % datamappingVersion,
      "io.projectreactor" % "reactor-core" % "3.4.22",
      "com.linecorp.armeria" %% "armeria-scala" % "1.17.1",
      "com.linecorp.armeria" % "armeria-logback" % "1.17.1",
      "org.slf4j" % "slf4j-api" % "2.0.0-alpha7",
      "ch.qos.logback" % "logback-classic" % "1.3.0-alpha16" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )

lazy val nexus = "https://dev2.greenfossil.com:8001/repository/"

parallelExecution := false

ThisBuild / resolvers ++= Seq(
  "GF Release" at nexus + "public/",
  "GF Snapshot" at nexus + "public-snapshots/"
)

ThisBuild / publishTo := {
  if (isSnapshot.value) Some("snapshots" at nexus + "snapshots/")
  else Some("releases"  at nexus + "releases/")
}

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
