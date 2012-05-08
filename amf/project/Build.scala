import sbt._
import Keys._
import PlayProject._
import scala.io.Source

object ApplicationBuild extends Build {

  val appName = "amf"
  val appVersion = "1.0"

  val appDependencies = Seq(
    "com.exadel.flamingo.flex" % "amf-serializer" % "1.5.0")

  lazy val loadedCredentials = {
    val line = Source.fromFile("credentials").getLines.toSeq
    Credentials(line(0), line(1), line(2), line(3))
  }
    
  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    organization := "nl.rhinofly",

    publishTo := Some(loadedCredentials.realm at loadedCredentials.host),

    credentials += loadedCredentials)

}
