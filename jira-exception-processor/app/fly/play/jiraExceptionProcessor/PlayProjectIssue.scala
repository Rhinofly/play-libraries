package fly.play.jiraExceptionProcessor

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.Json.{ toJson, fromJson }
import fly.play.jira.Issue
import fly.play.jira.IssueType
import fly.play.jira.Bug
import fly.play.jira.DefaultFormats

case class PlayProjectIssue(
  key: Option[String],
  summary: String,
  description: String,
  website: String,
  hash: String) extends Issue {

  val tpe: IssueType = Bug
  val project: String = "PLAY"

  //Website -> tests
  val customFieldValues = Map(Website.key -> Seq(website), Hash.key -> Seq(hash))
}

object PlayProjectIssue extends ((Option[String], String, String, String, String) => PlayProjectIssue) with DefaultFormats {
  implicit object playProjectIssueFormat extends Format[PlayProjectIssue] {
    def reads(json: JsValue): PlayProjectIssue = {
      val issue = fromJson[Issue](json)
      val customValues = Map[String, Seq[String]]()
      PlayProjectIssue(
        issue.key,
        issue.summary,
        issue.description,
        issue.customFieldValues(Website.key).head,
        issue.customFieldValues(Hash.key).head)
    }

    def writes(i: PlayProjectIssue): JsValue = toJson[Issue](i)
  }
}

object Hash {
  val key = "customfield_10490"
}

object Website {
  val key = "customfield_10080"
}