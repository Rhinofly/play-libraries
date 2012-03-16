package controllers
import play.api.mvc.Controller
import play.api.mvc.Action
import fly.play.facebook.Facebook
import play.api.mvc.Session
import play.api.mvc.Result

object FacebookController extends Controller {
  def callback(error: String, error_reason: String) = Action { implicit request =>
    var newSession = session

    val redirect = {
      val accessDenied = (error, error_reason) match {
        case ("access_denied", "user_denied") => Some(Redirect("SOME ERROR THINGY"))
        case _ => None
      }

      accessDenied.getOrElse {
        newSession += (Facebook.keys.accessToken -> request.body.asText.get)
        Redirect(session.get(Facebook.keys.originatingCall).getOrElse("/"))
      }
    }

    redirect.withSession(newSession
      - Facebook.keys.originatingCall
      - Facebook.keys.accessDeniedCall)
  }
}