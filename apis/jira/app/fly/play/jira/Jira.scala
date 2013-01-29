package fly.play.jira

import play.api.Play.current
import play.api.libs.concurrent.Promise
import play.api.libs.json.{ Format, JsObject }
import play.api.libs.json.Json.toJson
import play.api.libs.json.Reads
import play.api.libs.ws.{ Response, WS }

import com.ning.http.client.Realm.AuthScheme
import fly.play.libraryUtils.PlayConfiguration

object Jira extends DefaultFormats {

  /**
   * The endpoint, for example: https://jira.rhinofly.net/rpc/json-rpc/jirasoapservice-v2/
   */
  val endpoint = PlayConfiguration("jira.endpoint")
  val apiUsername = PlayConfiguration("jira.username")
  val apiPassword = PlayConfiguration("jira.password")

  protected def request(relativePath: String) = {
    val completeUrl = endpoint + relativePath
    println("Complete URL: %s" format completeUrl)
    WS
    .url(completeUrl)
    .withAuth(apiUsername, apiPassword, AuthScheme.BASIC)
  }

  def handleResponse[T](handler: PartialFunction[(Int, Response), Either[Error, T]])(response: Response): Either[Error, T] = {
    val defaultHandler: PartialFunction[(Int, Response), Either[Error, T]] = {
      case (400, response) => Left(response.json.as[Error])
      case (other, response) => {
        println(response.body)
        Left(Error(other, "Unknown error", None))
      }
    }

    (handler orElse defaultHandler)(response.status, response)
  }

  def addComment(issueKey: String, comment: String): Promise[Either[Error, Success]] = {
    val body = JsObject(List(
      "body" -> toJson(comment)))

    request("issue/%s/comment" format issueKey).post(body) map handleResponse {
      case (201, _) => Right(Success)
    }
  }

  def findIssues(query: String): Promise[Either[Error, Seq[Issue]]] = findIssuesAs[Issue](query)

  def findIssuesAs[T](query: String)(implicit reads: Reads[T]): Promise[Either[Error, Seq[T]]] = {
    request("search")
      .withQueryString("jql" -> query).get() map handleResponse {
        case (200, response) => Right(response.json.as[Seq[T]])
      }
  }

  def deleteIssue(issueKey: String): Promise[Either[Error, Success]] =
    request("issue/%s" format issueKey).delete() map handleResponse {
      case (204, _) => Right(Success)
    }

  def createIssue[T <: Issue](issue: T)(implicit format: Format[T]): Promise[Either[Error, T]] = {
    val body = toJson(issue)

    val theRequest = request("issue")
    println("Request: " + theRequest)
    theRequest.post(body) map handleResponse {
      case (200, response) => Right(response.json.as[T])
    }
  }
}