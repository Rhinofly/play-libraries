import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName         = "api-utils"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
  )
  
  val main = PlayProject(appName, appVersion, appDependencies).settings(
    organization := "nl.rhinofly",
    publishTo := Some("Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-snapshot-local"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )
}
