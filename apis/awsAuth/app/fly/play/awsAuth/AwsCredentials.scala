package fly.play.awsAuth

trait AwsCredentials {
  def accessKeyId:String
  def secretKey:String
  def sessionToken:Option[String]
}

object AwsCredentials {
  def unapply(c:AwsCredentials):Option[(String, String, Option[String])] = if (c == null) None else Some((c.accessKeyId, c.secretKey, c.sessionToken))
}

case class SimpleAwsCredentials(accessKeyId:String, secretKey:String, sessionToken:Option[String]) extends AwsCredentials