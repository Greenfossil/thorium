lazy val nexus = "https://dev2.greenfossil.com:8001/repository/"

ThisBuild / resolvers ++= Seq(
  "GF Release" at nexus + "public/",
  "GF Snapshot" at nexus + "public-snapshots/"
)

ThisBuild / publishTo := {
  if (isSnapshot.value) Some("snapshots" at nexus + "snapshots/")
  else Some("releases"  at nexus + "releases/")
}

ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")