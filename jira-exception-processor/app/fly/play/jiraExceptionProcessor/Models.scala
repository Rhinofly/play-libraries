package fly.play.jiraExceptionProcessor

import play.api.libs.json.Reads
import play.api.libs.json.JsValue
import play.api.Play.current
import play.api.Application
import play.api.libs.json.Format
import play.api.libs.json.Json.toJson
import play.api.libs.json.JsObject
import play.api.libs.json.JsArray

case class Error(status: Int, messages: Seq[String])

object Error {
  def fromJson(status: Int, json: JsValue) = Error(status, (json \ "errorMessages").as[Seq[String]])
}

trait Success
object Success extends Success

case class PlayProjectIssue(
  key: Option[String],
  summary: Option[String],
  description: Option[String],
  hash: Option[String]) {

}

object PlayProjectIssue {
  
  implicit object format extends Format[PlayProjectIssue] {
    def reads(json: JsValue) = {

      val fields = json \ "fields"

      PlayProjectIssue(
        (json \ "key").asOpt[String],
        (fields \ "summary").asOpt[String],
        (fields \ "description").asOpt[String],
        (fields \ Jira.hashCustomField).asOpt[String])
    }

    def writes(playProjectIssue: PlayProjectIssue) = {

      def field(pairs: (String, String)*): JsObject =
        JsObject(pairs.map { case (key, value) => key -> toJson(value) })
      def map(pairs: (String, JsValue)*): JsObject =
        JsObject(pairs)

      map(
        "fields" -> map(
          "project" -> field("id" -> Jira.projectId),
          "summary" -> toJson(playProjectIssue.summary),
          "description" -> toJson(playProjectIssue.description),
          "issuetype" -> field("id" -> Jira.issueType),
          "components" -> JsArray(Seq(field("id" -> Jira.componentId))),
          Jira.hashCustomField -> toJson(playProjectIssue.hash)))
    }
  }
}