import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "api-dynamoDb"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
      "nl.rhinofly" %% "api-aws-utils" % "1.0-SNAPSHOT",
      "nl.rhinofly" %% "api-sts" % "1.0-SNAPSHOT"
  )

  val rhinoflyRepo = "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-snapshot-local"

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    organization := "nl.rhinofly",
    resolvers += rhinoflyRepo,
    publishTo := Some(rhinoflyRepo),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"))

}
