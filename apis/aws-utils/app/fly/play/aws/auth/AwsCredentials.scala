package fly.play.aws.auth

import java.util.Date
import play.api.Play.current
import fly.play.libraryUtils.PlayConfiguration

trait AwsCredentials {
  def accessKeyId: String
  def secretKey: String
  def sessionToken: Option[String]
  def expiration: Option[Date]
}

object AwsCredentials extends ((String, String, Option[String], Option[Date]) => AwsCredentials) {
  def unapply(c: AwsCredentials): Option[(String, String, Option[String], Option[Date])] = if (c == null) None else Some((c.accessKeyId, c.secretKey, c.sessionToken, c.expiration))

  def apply(accessKeyId: String, secretKey: String, sessionToken: Option[String] = None, expiration: Option[Date] = None): AwsCredentials =
    SimpleAwsCredentials(accessKeyId, secretKey, sessionToken, expiration)

  lazy val fromConfiguration: AwsCredentials = SimpleAwsCredentials(PlayConfiguration("aws.accessKeyId"), PlayConfiguration("aws.secretKey"))
  
  implicit def implicitAwsCredentials:AwsCredentials = fromConfiguration
}

case class SimpleAwsCredentials(accessKeyId: String, secretKey: String, sessionToken: Option[String] = None, expiration: Option[Date] = None) extends AwsCredentials