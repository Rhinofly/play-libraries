package fly.play.jira

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.Json.{ toJson }
import play.api.libs.json.JsString
import play.api.libs.json.Reads
import play.api.libs.json.JsArray
import play.api.libs.json.Json

trait Issue {
  def tpe: IssueType
  def key: Option[String]
  def project: String
  def summary: String
  def description: String
  def customFieldValues: Map[String, Seq[String]]
}

case class SimpleIssue(tpe: IssueType, key: Option[String], project: String, summary: String, description: String, customFieldValues: Map[String, Seq[String]]) extends Issue

object Issue extends ((IssueType, Option[String], String, String, String, Map[String, Seq[String]]) => Issue) with DefaultFormats {
  def apply(tpe: IssueType, key: Option[String], project: String, summary: String, description: String, customFieldValues: Map[String, Seq[String]] = Map()): Issue =
    SimpleIssue(tpe, key, project, summary, description, customFieldValues)
}

sealed abstract class IssueType(val id: Int)

case object Bug extends IssueType(1)

case class Error(code: Int, message: String, data: Option[String])

trait Success
object Success extends Success

trait DefaultFormats {

  implicit object issueFormat extends Format[Issue] {

    def readCustomValues(json: JsValue): Map[String, Seq[String]] = 
      json match {
        case x: JsArray => x.as[Seq[JsObject]].map { c =>
         (c \ "customfieldId").as[String] -> (c \ "values").as[Seq[String]]
        }.toMap
        case x => throw new Exception("Can not read custom values: " + x)
      }
    
    def writeCustomValues(customValues: Map[String, Seq[String]]): Option[JsValue] = {
      val values = 
        customValues.toList.map {
          case (key, value) =>
            JsObject(List(
              "customfieldId" -> toJson(key),
              "values" -> toJson(value)))
        }
      
      if (values.isEmpty) None else Some(toJson(values))
    }

    def reads(json: JsValue): Issue = Issue(
      (json \ "type").as[IssueType],
      (json \ "key").asOpt[String],
      (json \ "project").as[String],
      (json \ "summary").as[String],
      (json \ "description").as[String],
      readCustomValues(json \ "customFieldValues"))

    def writes(i: Issue): JsValue = JsObject(List(
      "type" -> toJson(i.tpe),
      "project" -> toJson(i.project),
      "summary" -> toJson(i.summary),
      "description" -> toJson(i.description)
      ) 
      ::: i.key.map("key" -> toJson(_)).toList 
      ::: writeCustomValues(i.customFieldValues).map("customFieldValues" -> _).toList)
  }

  implicit lazy val issueTypeFormat: Format[IssueType] = new Format[IssueType] {
    def reads(json: JsValue): IssueType = json.as[String] match {
      case "1" => Bug
      case x => throw new Exception("No issue type for " + x + ", please add it to the model class")
    }

    def writes(i: IssueType): JsValue = JsNumber(i.id)
  }

  implicit lazy val errorReads: Reads[Error] = new Reads[Error] {
    def reads(json: JsValue): Error = {
      val error = (json \ "error")
      Error(
        (error \ "code").as[Int],
        (error \ "message").as[String],
        (error \ "data").asOpt[String])

    }
  }
}