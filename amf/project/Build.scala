import sbt._
import Keys._
import PlayProject._
import scala.io.Source

object ApplicationBuild extends Build {

  val appName = "amf"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.exadel.flamingo.flex" % "amf-serializer" % "1.5.0")

  val main = PlayProject(appName, appVersion, appDependencies).settings(
    organization := "nl.rhinofly",
    publishTo := Some("Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-snapshot-local"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )
}
