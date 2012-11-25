import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "flash-socket-policy-server"
  val appVersion = "1.0.0"

  val appDependencies = Seq(
    "org.mockito" % "mockito-core" % "1.9.5" % "test")

  def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }

  val main = PlayProject(appName, appVersion, appDependencies).settings(
    organization := "nl.rhinofly",
    publishTo <<= version(rhinoflyRepo),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"))

}
