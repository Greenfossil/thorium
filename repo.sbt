ThisBuild / organizationName := "Greenfossil Pte Ltd"
ThisBuild / organizationHomepage := Some(url("https://greenfossil.com/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/greenfossil/thorium"),
    "scm:git@github.com:greenfossil/thorium.git"
  )
)

ThisBuild / homepage := Some(url("https://github.com/Greenfossil/thorium"))

ThisBuild / description := "Microservices Framework built on top of Armeria, Java 17 and Scala 3"

ThisBuild / licenses := List(
  "Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
)

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true

ThisBuild / developers := List(
  Developer(
    "greenfossil",
    "Greenfossil Pte Ltd",
    "devadmin@greenfossil.com",
    url("https://github.com/Greenfossil")
  )
)

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

ThisBuild / credentials += {
  val ghCredentials = for {
    user <- sys.env.get("PUBLISH_USER").filter(_.nonEmpty)
    pass <- sys.env.get("PUBLISH_PASSWORD").filter(_.nonEmpty)
  } yield Credentials("Sonatype Nexus Repository Manager", "central.sonatype.com", user, pass)

  val fileCredentials = {
    val credFile = Path.userHome / ".sbt" / "sonatype_central_credentials"
    if (credFile.exists) Some(Credentials(credFile)) else None
  }

  ghCredentials
    .orElse(fileCredentials)
    .getOrElse {
      Credentials("Sonatype Nexus Repository Manager", "central.sonatype.com", "", "")
    }
}



lazy val sonatypeStaging = "https://s01.oss.sonatype.org/content/groups/staging/"

ThisBuild / resolvers ++= Seq(
  "Sonatype Staging" at sonatypeStaging
)