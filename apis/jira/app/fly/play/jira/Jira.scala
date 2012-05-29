package fly.play.jira

import fly.play.libraryUtils.PlayConfiguration
import play.api.libs.concurrent.Promise
import play.api.Play.current
import play.api.http.ContentTypeOf
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{ toJson, fromJson }
import play.api.libs.ws.Response
import play.api.libs.ws.WS
import play.api.libs.json.JsObject
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.Format

object Jira extends DefaultFormats {

  /**
   * The endpoint, for example: https://jira.rhinofly.net/rpc/json-rpc/jirasoapservice-v2/
   */
  lazy val endpoint = PlayConfiguration("jira.endpoint")

  //The default json content type is not accepted because it contains the encoding (blame Jira) 
  implicit val contentType = new ContentTypeOf[JsValue](Some("application/json"))

  def call[T](method: String, arguments: JsValue)(converter: JsValue => T): Promise[Either[Error, T]] = 
    WS
      .url(endpoint + method)
      .post(arguments)
      .map { r =>
        r.status match {
          case 200 => r.json match {
	          case error: JsObject if (error \ "error").asOpt[JsObject] != None => Left(fromJson[Error](error))
	          case x => Right(converter(x))
	          }
          case x => Left(Error(x, "Unknown error", None))
        }
      }

  def callWithToken[T](token:String, method: String, arguments: JsValue)(converter: JsValue => T): Promise[Either[Error, T]] =
    call(method, JsArray(List(JsString(token), arguments)))(converter)
    
  def callWithToken[T](token:Promise[Either[Error, String]], method: String, arguments: JsValue)(converter: JsValue => T): Promise[Either[Error, T]] =
    token.flatMap {
      case Right(token) =>
        callWithToken(token, method, arguments)(converter)
    }

  def callWithToken[T](method: String, arguments: JsValue)(converter: JsValue => T): Promise[Either[Error, T]] = 
    callWithToken(token, method, arguments)(converter)
  
  def token(username:String, password:String): Promise[Either[Error, String]] = 
    call("login", toJson(Seq(username, password)))(_.as[String])
    
  def token: Promise[Either[Error, String]] = 
    token(PlayConfiguration("jira.username"), PlayConfiguration("jira.password"))

  def createIssue[T <: Issue](issue: T)(implicit format:Format[T]): Promise[Either[Error, T]] =
    callWithToken("createIssue", toJson(issue))(_.as[T])

  def deleteIssue(issueKey:String):Promise[Either[Error, Success]] =
    callWithToken("deleteIssue", toJson(issueKey))(json => Success)
    	//With a successful delete we get a 404... Jira bug
    	.map{case Left(Error(404, _, _)) => Right(Success)}
}