val scala3Version = "3.1.1"

lazy val armeriaVersion = "1.15.0"
lazy val logbackVersion = "1.2.10"
lazy val munitVersion = "0.7.29"

lazy val webServer = project
  .in(file("."))
  .settings(
    name := "web-server",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.greenfossil" %% "commons-json" % "0.1.0-RC4",
      "com.linecorp.armeria" %% "armeria-scala" % armeriaVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime,
      "org.scalameta" %% "munit" % munitVersion % Test
    )
  )

lazy val nexus = "https://dev2.greenfossil.com:8001/repository/"

resolvers ++= Seq(
  "GF Release" at nexus + "public/",
  "GF Snapshot" at nexus + "public-snapshots/"
)

publishTo := {
  if (isSnapshot.value) Some("snapshots" at nexus + "snapshots/")
  else Some("releases"  at nexus + "releases/")
}
