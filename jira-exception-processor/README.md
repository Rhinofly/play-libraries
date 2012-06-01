This module is created for internal use. If there is any interest in this feature for play, please contact us so we 
can make it more portable. Currently the two custom fields are hardcoded.

```` scala
jira.username=username
jira.password=password
jira.endpoint="https://jira.company.net/rpc/json-rpc/jirasoapservice-v2/"

jira.play.website=website 

mail.from.name=Play application
mail.from.address="noreply@rhinofly.net"
mail.smtp.failTo="failto@rhinofly.net"

mail.smtp.host=email-smtp.us-east-1.amazonaws.com
mail.smtp.port=465
mail.smtp.username="username"
mail.smtp.password="password"

jiraExceptionProcessor.enabled=false
```

Usage:

``` scala

object Global extends GlobalSettings {
	override def onError(request:RequestHeader, ex:Throwable) = {
	  JiraExceptionProcessor.reportError(request, ex)
	  super.onError(request, ex)
	}
}
```