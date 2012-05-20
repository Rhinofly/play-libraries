This project contains a set of libraries that can be used with Play Framework 2.0. We are currently only focussing on the Scala side of things.

Currently all of these libraries are under development. 

The libraries can be obtained using the following snapshot repository: http://maven-repository.rhinofly.net:8081/artifactory/libs-snapshot-local

Note that these libraries may change drastically as long as they have 1.0-SNAPSHOT as version.

You can use the projects in two ways:

1. Download the project and execute `play publish-local`
2. Add the snapshot repository as a resolver in your `Build.scala` file:

``` scala
val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-snapshot-local"
)
```

The following libraries are available:

- [amf](tree/master/amf) ActionScript Message Format
- [s3](tree/master/apis/s3) Amazon Simple Storage Service
- [ses](tree/master/apis/ses) Amazon Simple Email Service
- [aws-utils](tree/master/apis/aws-utils) Amazon AWS utils (for example Version 4 signing)
- [sts](tree/master/apis/sts) Amazon Security Token Service