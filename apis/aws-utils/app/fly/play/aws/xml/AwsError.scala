package fly.play.aws.xml

import scala.xml.Elem

case class AwsError(status: Int, code: String, message: String, originalXml:Option[Elem]) {
	def this(status: Int, originalXml:Elem) = this(status, originalXml \ "Code" text, originalXml \ "Message" text, Some(originalXml))
}

object AwsError extends ((Int, String, String, Option[Elem]) => AwsError) {
  def apply(status:Int, originalXml:Elem) = new AwsError(status, originalXml)
}