package fly.play.aws.auth

import fly.play.utils.PlayUtils.playConfiguration
import java.util.Date
import play.api.Play.current

trait AwsCredentials {
  def accessKeyId:String
  def secretKey:String
  def sessionToken:Option[String]
  def expiration:Option[Date]
}

object AwsCredentials {
  def unapply(c:AwsCredentials):Option[(String, String, Option[String], Option[Date])] = if (c == null) None else Some((c.accessKeyId, c.secretKey, c.sessionToken, c.expiration))
  
  def fromConfiguration:AwsCredentials = SimpleAwsCredentials(playConfiguration("aws.accessKeyId"), playConfiguration("aws.secretKey"))
}

case class SimpleAwsCredentials(accessKeyId:String, secretKey:String, sessionToken:Option[String] = None, expiration:Option[Date] = None) extends AwsCredentials