Play 2.0 module that supplies a Session Cache
=============================================

This library supplies a wrapper for actions that can be used to associate a 
cache with a specific user. This is similar to the session in servlet type 
applications.

Under the hood the cache that is provided is nothing more than a wrapper 
around the default Play 2 Cache that makes sure that every key is supplied 
with a prefix.

If the user has no 'id' associated (no sessionId cookie), a cookie will be 
set. When the user has an 'id', that 'id' will be used to create the cache 
wrapper. 

Since we are using the default Cache, it's up to you to specify timeouts. 
Unlike servlet type session mechanisms there is no global timeout. Even 
the cookie does not have a limit, it simply contains an 'id'.


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
      "nl.rhinofly" %% "session-cache" % "1.0.0"
    )
```

Usage 
------------

``` scala
object Application extends Controller {

  def index = SessionCache { sessionCache =>
    Action {
      val userIdOption = sessionCache get "userId"
    
      Ok(views.html.index(someValueFromTheCache))
    }
  }
  
}
```