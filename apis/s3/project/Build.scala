import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "api-s3"
  val appVersion = "1.7.0"

  val appDependencies = Seq(
    "nl.rhinofly" %% "api-aws-utils" % "1.4.0")

  def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }

  val main = play.Project(appName, appVersion, appDependencies ).settings(
    organization := "nl.rhinofly",
    resolvers += rhinoflyRepo("RELEASE").get,
    publishTo <<= version(rhinoflyRepo),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
	scalacOpts)
	
  lazy val scalacOpts = scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature")

}
