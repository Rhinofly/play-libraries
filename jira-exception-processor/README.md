Jira exception processor module for Play 2.0
============================================

This module is created for internal use. If there is any interest in this feature for play, please contact us so we 
can make it more portable. Currently the two custom jira fields (Hash and Website) are hardcoded.

Installation
------------

In the `Build.scala` file add the dependency

``` scala
  val appDependencies = Seq(
    "nl.rhinofly" %% "jira-exception-processor" % "1.0")

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local")

  }
```

Configuration
-------------

In the `application.conf` file add the following pieces of information:

``` scala
#Jira information, needed to report the errors to Jira
jira.username=username
jira.password=password
jira.endpoint="https://jira.company.net/rpc/json-rpc/jirasoapservice-v2/"

#Mail configuration in case the reporting to Jira fails
mail.from.name=Play application
mail.from.address="noreply@rhinofly.net"
mail.smtp.failTo="failto@rhinofly.net"

mail.smtp.host=email-smtp.us-east-1.amazonaws.com
mail.smtp.port=465
mail.smtp.username="username"
mail.smtp.password="password"

#Information needed by the exception processor, note that enabled needs to be changed to true in production
jira.play.website=website 
jiraExceptionProcessor.enabled=false
```


Usage
-----

Create a `Global.scala` file in the root package with the following contents:

``` scala
object Global extends GlobalSettings {
	override def onError(request:RequestHeader, ex:Throwable) = {
	  JiraExceptionProcessor.reportError(request, ex)
	  super.onError(request, ex)
	}
}
```





