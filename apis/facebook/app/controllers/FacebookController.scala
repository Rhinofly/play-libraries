package controllers
import play.api.mvc.Controller
import play.api.mvc.Action
import fly.play.facebook.Facebook
import play.api.mvc.Session
import play.api.mvc.Result

object FacebookController extends Controller {
  def callback(code: Option[String], error: Option[String], error_reason: Option[String]) = Action { implicit request =>
    var newSession = session
    println(error)
    println(error_reason)
    val redirect = (code, error, error_reason) match {
      case (None, Some("access_denied"), Some("user_denied")) => Redirect(session.get(Facebook.keys.accessDeniedCall).get)
      case (Some(code), None, None) => {
        newSession += (Facebook.keys.accessToken -> code)
        Redirect(session.get(Facebook.keys.originatingCall).getOrElse("/"))
      }
      case parameters => BadRequest(parameters.toString)
    }

    redirect.withSession(newSession
      - Facebook.keys.originatingCall
      - Facebook.keys.accessDeniedCall)
  }
}