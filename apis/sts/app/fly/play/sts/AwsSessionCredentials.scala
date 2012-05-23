package fly.play.sts

import play.api.cache.Cache
import play.api.Play.current
import java.util.Date
import fly.play.aws.auth.AwsCredentials
import fly.play.aws.xml.AwsError
import fly.play.aws.AwsException

case class AwsSessionCredentials(credentials:AwsCredentials, durationSeconds:Int = 3600) extends AwsCredentials{
  
  //TODO use session cache
  def sessionCredentials:AwsCredentials = 
    Cache.getOrElse("aws.session.credentials", durationSeconds - 60) {
      Sts.sessionToken(credentials, durationSeconds).value.get match {
        case Left(AwsError(status, code, message, originalXml)) => throw AwsException(status, code, message, Some(originalXml.toString))
        case Right(credentials:AwsCredentials) => credentials
      }
    }
  
  def accessKeyId = sessionCredentials.accessKeyId
  def secretKey = sessionCredentials.secretKey
  def sessionToken = sessionCredentials.sessionToken
  def expiration = sessionCredentials.expiration
  
}