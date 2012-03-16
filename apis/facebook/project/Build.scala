import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "facebook"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "org.scribe" % "scribe" % "1.3.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
