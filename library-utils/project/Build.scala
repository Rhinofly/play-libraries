import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "library-utils"
  val appVersion = "1.0.1"

  val appDependencies = Seq( // Add your project dependencies here,
  )

  def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }

  val main = PlayProject(appName, appVersion, appDependencies).settings(
    organization := "nl.rhinofly",
    publishTo <<= version(rhinoflyRepo),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"))

}
