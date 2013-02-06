Jira exception processor module for Play 2.0.4
============================================

This module is created for internal use. If there is any interest in this feature for play, please contact us so we 
can make it more portable.

Installation
------------

In the `Build.scala` file add the dependency

``` scala
  val appDependencies = Seq(
    "nl.rhinofly" %% "jira-exception-processor" % "2.0.1")

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local")

  }
```

Configuration
-------------

In the `application.conf` file add the following pieces of information:

``` scala
# Jira information, needed to report the errors to Jira
jira.username=username
jira.password="password"
jira.endpoint="https://rhinofly.atlassian.net/rest/api/2/"

# Information needed by the exception processor
jira.exceptionProcessor.enabled=true
jira.exceptionProcessor.projectKey=PA
jira.exceptionProcessor.componentName=tests
# Hash is the default
#jira.exceptionProcessor.hashCustomFieldName=Hash
# 1 is the default (Bug)
#jira.exceptionProcessor.issueType=1

# Used when the connection to Jira failed, note that the error is also logged
jira.exceptionProcessor.mail.from.name=Play application
jira.exceptionProcessor.mail.from.address="noreply@rhinofly.net"
jira.exceptionProcessor.mail.to.name=Play
jira.exceptionProcessor.mail.to.address="play+error@rhinofly.nl"

# Used by the SES plugin
mail.smtp.failTo="failto+test@rhinofly.net"

mail.smtp.host=email-smtp.us-east-1.amazonaws.com
mail.smtp.port=465
mail.smtp.username="username"
mail.smtp.password="password"
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

Testing
-------

In order to test put the above configuration in `/test/conf/application.conf`. 
Note that this directory is present in `.gitignore` in order to prevent credentials 
from ending up in Github.




