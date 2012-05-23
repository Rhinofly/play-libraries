package fly.play.sts

import scala.xml.Elem
import fly.play.aws.auth.AwsCredentials
import java.util.Date
import fly.play.aws.Aws

case class AwsXmlCredentials(xml:Elem) extends AwsCredentials {
  private val credentials = xml \ "GetSessionTokenResult" \ "Credentials"
  
  val accessKeyId:String = credentials \ "AccessKeyId" text
  def secretKey:String = credentials \ "SecretAccessKey" text
  def sessionToken:Option[String] = Some(credentials \ "SessionToken" text)
  def expiration:Option[Date] = Some(Aws.dates.iso8601DateFormat.parse(credentials \ "Expiration" text))
}