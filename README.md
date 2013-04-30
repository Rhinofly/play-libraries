*Job opening: Scala programmer at Rhinofly*
-------------------------------------------
Each new project we start is being developed in Scala. Therefore, we are in need of a [Scala programmer](http://rhinofly.nl/vacature-scala.html) who loves to write beautiful code. No more legacy projects or maintenance of old systems of which the original programmer is already six feet under. What we need is new, fresh code for awesome projects.

Are you the Scala programmer we are looking for? Take a look at the [job description](http://rhinofly.nl/vacature-scala.html) (in Dutch) and give the Scala puzzle a try! Send us your solution and you will be invited for a job interview.
* * *

This project contains a set of libraries that can be used with Play Framework 2.0. We are currently only focusing on the Scala side of things.

Currently most of these libraries are under development. Each time a stable version is finished it is published to the release repository at http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local. Latest development versions can be obtained from the snapshot repository at http://maven-repository.rhinofly.net:8081/artifactory/libs-snapshot-local.

Note that these libraries may change drastically as long as they have 1.0-SNAPSHOT as version.

You can use the projects in two ways:

1. Download the project and execute `play publish-local`
2. Add the snapshot (or release) repository as a resolver in your `Build.scala` file:

``` scala
val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-snapshot-local"
)
```

The following libraries are available:

- [amf](/Rhinofly/play-libraries/tree/master/amf) ActionScript Message Format
- [s3](/Rhinofly/play-libraries/master/apis/s3) Amazon Simple Storage Service
- [ses](/Rhinofly/play-libraries/master/apis/ses) Amazon Simple Email Service
- [aws-utils](/Rhinofly/play-libraries/master/apis/aws-utils) Amazon AWS utils (for example Version 4 signing)
- [sts](/Rhinofly/play-libraries/master/apis/sts) Amazon Security Token Service

Running tests
-------------

A lot of these projects require settings defined in the `application.conf` file. These might contain sensitive information (access keys and secrets). In order to run the tests you need create a directory called `conf` within the `test` directory and place an `application.conf` inside. This is already added to the `.gitignore` file and thus can not be accidentally commited.
