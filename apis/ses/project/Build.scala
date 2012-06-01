import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "api-ses"
  val appVersion = "1.0"

  val appDependencies = Seq(
    "javax.mail" % "mail" % "1.4",
    "nl.rhinofly" %% "library-utils" % "1.0")

  def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    organization := "nl.rhinofly",
    resolvers += rhinoflyRepo("RELEASE").get,
    publishTo <<= version(rhinoflyRepo),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"))

}
