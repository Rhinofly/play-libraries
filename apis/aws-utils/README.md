Amazon Web Services utils
=========================

This project contains utilities that are used in multiple Amazon services.

The Aws object
--------------

This object is mainly used to wrap Play's `WS` component and add signing to it. Usage is like this:

``` scala
Aws
  .withSigner(...)
  .url(...)
 	
  ...
 	
Aws
  .withSigner3(...)
  .url(...)
```

The Signers
-----------

The current version supplies Aws version 3 and Aws version 4 signers.

AwsCredentials
--------------

The `AwsCredentials` object contains `apply` and `unapply` methods. Some usage examples:

``` scala
val c = AwsCredentials("key", "secret")

val AwsCredentials(key, secret, tokenOption, expirationOption) = c

```

The `AwsCredentials` object allows you to create an instance using information in the configuration:

``` scala
# In the configuration
aws.accessKeyId=testKey
aws.secretKey=testSecret

//In Scala
val c = AwsCredentials.fromConfiguration
```

The `AwsCredentials` object also contains an implicit configuration (internally uses the `fromConfiguration` object.

``` scala

def myMethod(arg1:Int)(implicit credentials:AwsCredentials) = {
  //...
}
```

Installation
------------

``` scala
  val appDependencies = Seq(
    "nl.rhinofly" %% "api-aws-utils" % "1.1"
  )
  
  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Release Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"
  )
```
