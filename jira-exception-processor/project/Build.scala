import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "jira-exception-processor"
  val appVersion = "2.0.0"

  val appDependencies = Seq(
    "nl.rhinofly" %% "api-ses" % "1.0.1")

  def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }

  val main = PlayProject(appName, appVersion, appDependencies).settings(
    organization := "nl.rhinofly",
    publishTo <<= version(rhinoflyRepo),
    resolvers += rhinoflyRepo("RELEASE").get,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"))

}
