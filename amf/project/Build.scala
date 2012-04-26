import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "amf"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      "com.exadel.flamingo.flex" % "amf-serializer" % "1.5.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      organization := "nl.rhinofly"    
    )

}
