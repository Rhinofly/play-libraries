import sbt._
import Keys._

object ApplicationBuild extends Build {

    val appName         = "api-aws-utils"
    val appVersion      = "1.4.0"

    val appDependencies = Seq(
      "nl.rhinofly" %% "library-utils" % "1.2.0"
    )

    
 def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }
    
  val main = play.Project(appName, appVersion, appDependencies).settings(
    organization := "nl.rhinofly",
    publishTo <<= version(rhinoflyRepo),
    resolvers += rhinoflyRepo("RELEASE").get,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
	scalacOpts)
	
  lazy val scalacOpts = scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature")


}
