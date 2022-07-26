val scala3Version = "3.1.3"

lazy val commonsCryptoVersion = "0.1.0-RC3"
lazy val typesafeConfigExtVersion = "0.1.0-RC4"
lazy val datamappingVersion="0.1.0-RC12"

lazy val webServer = project
  .in(file("."))
  .settings(
    name := "web-server",
    organization := "com.greenfossil",
    version := "0.1.0-RC10",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17", "-target", "17"),

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "commons-crypto" % commonsCryptoVersion,
      "com.greenfossil" %% "typesafe-config-ext" % typesafeConfigExtVersion,
      "com.greenfossil" %% "data-mapping" % datamappingVersion,
      "com.linecorp.armeria" %% "armeria-scala" % "1.17.1",
      "ch.qos.logback" % "logback-classic" % "1.2.11" % Runtime,
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

credentials += Credentials("Sonatype Nexus Repository Manager", "dev2.greenfossil.com", "dev", "ayerrajah")
