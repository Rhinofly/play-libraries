package fly.play.sts

import play.api.libs.concurrent.Promise
import fly.play.aws.auth.AwsCredentials
import fly.play.aws.Aws
import fly.play.aws.xml.AwsResponse
import fly.play.aws.auth.SimpleAwsCredentials
import fly.play.aws.xml.AwsError
import play.api.libs.ws.Response
import fly.play.aws.auth.SimpleAwsCredentials
import play.api.libs.ws.WS

object Sts {
	def sessionToken(credentials:AwsCredentials, durationSeconds:Int = 3600):Promise[Either[AwsError, AwsCredentials]] = {
	  Aws
	  	.withSigner4(Some(credentials), service = Some("sts"))
	  	.url("https://sts.amazonaws.com/")
	  	.withQueryString(
	  	    "Version" -> "2011-06-15",
	  	    "Action" -> "GetSessionToken",
	  	    "DurationSeconds" -> durationSeconds.toString )
	  	.get
	  	.map(credentialsReponse)
	}
	
	def credentialsReponse = AwsResponse { (status, response) => AwsXmlCredentials(response.xml) } _
}