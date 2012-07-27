package fly.play.aws.xml

import scala.xml.Elem
import scala.xml.NodeSeq
import AwsError.getErrorElem

case class AwsError(status: Int, code: String, message: String, originalXml: Option[Elem]) {
  
  def this(status: Int, originalXml: Elem) =
    this(status,
      getErrorElem(originalXml) \ "Code" text,
      getErrorElem(originalXml) \ "Message" text,
      Some(originalXml))
      
}

object AwsError extends ((Int, String, String, Option[Elem]) => AwsError) {
  def apply(status: Int, originalXml: Elem) = new AwsError(status, originalXml)
  private def getErrorElem(originalXml: Elem):NodeSeq = (if (originalXml.label == "Error") originalXml else originalXml \ "Error")
}