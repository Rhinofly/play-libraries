package fly.play.jira

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.Json.{toJson, fromJson}

case class PlayProjectIssue(
    key:Option[String], 
    summary:String, 
    description:String,
    website:Website) extends Issue {
  
  val tpe:IssueType = Bug
  val project:String = "PLAY"
    
  //Website -> tests
  val customFieldValues = Map(Website.id -> Seq(website.id))  
}

sealed abstract class Website(val id:String)

object Website extends (String => Website) {
	val id = "customfield_10080"
  
	case object tests extends Website("tests") 
  
	def apply(id:String):Website = id match {
	  case tests.id => tests
	  case x => throw new Exception("Unknown website id: " + x)
	}
}


object PlayProjectIssue extends ((Option[String], String, String, Website) => PlayProjectIssue) with DefaultFormats {
  implicit object playProjectIssueFormat extends Format[PlayProjectIssue] {
    def reads(json:JsValue):PlayProjectIssue = {
      val issue = fromJson[Issue](json) 
      val customValues = Map[String, Seq[String]]()
      PlayProjectIssue(
        issue.key,
        issue.summary, 
        issue.description,
        Website(issue.customFieldValues(Website.id).head))
    }
    
    def writes(i:PlayProjectIssue):JsValue = toJson[Issue](i)
  }
}