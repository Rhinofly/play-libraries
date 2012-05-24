import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "api-sts"
  val appVersion = "1.0"

  val appDependencies = Seq(
      "nl.rhinofly" %% "api-aws-utils" % "1.0"
  )

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
