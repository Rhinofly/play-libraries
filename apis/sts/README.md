Api Security Token Service (STS) Module for Play 2.0
====================================================

A simple wrapper for the Amazon STS service used (for example) by DynamoDB.

Installation
------------

``` scala
  val appDependencies = Seq(
    "nl.rhinofly" %% "api-sts" % "1.0"
  )
  
  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Release Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"
  )
```

Usage
-----

``` scala
   