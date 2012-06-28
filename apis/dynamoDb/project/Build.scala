import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "api-dynamoDb"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
      "nl.rhinofly" %% "api-sts" % "1.2-SNAPSHOT"
  )

  def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }
  
  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    organization := "nl.rhinofly",
    resolvers += rhinoflyRepo("RELEASE").get,
    resolvers += rhinoflyRepo("SNAPSHOT").get,
    publishTo <<= version(rhinoflyRepo),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"))

}
