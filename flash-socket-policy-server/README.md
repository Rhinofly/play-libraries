Play 2.0 module that supplies a Flash Socket Policy Server 
==========================================================

Small library module that you can use to startup a flash socket policy server at 
port 843. Note that this is only useful if this is the only play application that 
has this module. It's very unlikely that you can use it with 3rd party hosting. 

Note that this is developed with and for *Scala*.

Installation
------------

Add a resolver to your project settings:

``` scala
val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"
)
```

Add the dependency:

``` scala
	val appDependencies = Seq(
      "nl.rhinofly" %% "flash-socket-policy-server" % "1.0.0"
    )
```

Usage 
------------

``` scala
import play.api._
import fly.play.flashSocketPolicyServer.FlashSocketPolicyServer

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    FlashSocketPolicyServer.start
    super.onStart(app)
  }

  override def onStop(app: Application) {
    FlashSocketPolicyServer.stop
    super.onStop(app)
  }
  
}
```