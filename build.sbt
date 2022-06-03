val scala3Version = "3.1.1"

lazy val armeriaVersion = "1.16.0"
lazy val logbackVersion = "1.2.10"
lazy val munitVersion = "0.7.29"
lazy val commonsCryptoVersion = "0.1.0-RC2"

lazy val webServer = project
  .in(file("."))
  .settings(
    name := "web-server",
    organization := "com.greenfossil",
    version := "0.1.0-RC3-SNAPSHOT",

    scalaVersion := scala3Version,

    Compile / javacOptions ++= Seq("-source", "17", "-target", "17"),

    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.2",
      "com.greenfossil" %% "data-mapping" % "0.1.0-RC5",
      "com.greenfossil" %% "commons-crypto" % commonsCryptoVersion,
      "com.linecorp.armeria" %% "armeria-scala" % armeriaVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime,
      "org.scalameta" %% "munit" % munitVersion % Test
    )
  )

lazy val nexus = "https://dev2.greenfossil.com:8001/repository/"

ThisBuild / resolvers ++= Seq(
  "GF Release" at nexus + "public/",
  "GF Snapshot" at nexus + "public-snapshots/"
)

ThisBuild / publishTo := {
  if (isSnapshot.value) Some("snapshots" at nexus + "snapshots/")
  else Some("releases"  at nexus + "releases/")
}

credentials += Credentials("Sonatype Nexus Repository Manager", "dev2.greenfossil.com", "dev", "ayerrajah")