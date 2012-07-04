Amazon DynamoDB module for Play 2.0
=====================================================

API for the Amazon DynamoDB service 


Installation
------------

``` scala
  val appDependencies = Seq(
    "nl.rhinofly" %% "api-dynamoDb" % "1.0"
  )
  
  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Release Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"
  )
```

Configuration
-------------

`application.conf` should contain the following information:

``` scala

```

Usage
-----