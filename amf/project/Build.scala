import sbt._
import Keys._
import PlayProject._
import scala.io.Source

object ApplicationBuild extends Build {

  val appName = "amf"
  val appVersion = "1.0"

  val appDependencies = Seq(
    "com.exadel.flamingo.flex" % "amf-serializer" % "1.5.0")

  def rhinoflyRepo(version:String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
	Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }
    
  val main = PlayProject(appName, appVersion, appDependencies).settings(
    organization := "nl.rhinofly",
    publishTo <<= version(rhinoflyRepo),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )
}
