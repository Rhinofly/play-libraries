package fly.play.facebook
import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.Play
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import org.scribe.builder.ServiceBuilder
import play.Configuration
import org.scribe.builder.api.{ FacebookApi => ScribeFacebookApi }
import play.api.mvc.Call
import controllers.routes
import play.api.mvc.RequestHeader
import org.scribe.model.Verifier
import scala.collection.mutable.ListBuffer
import play.api.libs.json.JsObject
import play.api.libs.json.Reads
import scala.collection.mutable.HashMap
import play.api.Application
import play.api.PlayException

trait FacebookApi { self: Controller =>

  val accessDeniedAction: Call

  private def callFromRequest(implicit requestHeader: RequestHeader) =
    Call(requestHeader.method, requestHeader.path + "?" + requestHeader.rawQueryString)

  private def facebookServiceBuilder()(implicit requestHeader: RequestHeader) = new ServiceBuilder()
    .provider(classOf[ScribeFacebookApi])
    .apiKey(Facebook.keys.appId)
    .apiSecret(Facebook.keys.appSecret)
    .callback(routes.FacebookController.callback(None, None, None).absoluteURL())

  def FacebookAuthenticated[T <: FacebookObject, A](action: T => Action[A])(implicit f: FacebookObjectInformation[T], m: Manifest[T]): Action[(Action[A], A)] = {
     
    val authenticatedBodyParser = BodyParser { implicit request =>
      
      session.get(Facebook.keys.accessToken).map { facebookAccessToken =>
        val facebookUser = manifest[T].erasure.newInstance.asInstanceOf[T]
        facebookUser.accessToken = Some(facebookAccessToken)
        val innerAction = action(facebookUser)
        innerAction.parser(request).mapDone { body =>
          body.right.map(innerBody => (innerAction, innerBody))
        }
      }.getOrElse {
         Done(Left {
          val builder = facebookServiceBuilder
          
          f.scopes.foreach(builder.scope(_))
          
          Redirect(facebookServiceBuilder.build().getAuthorizationUrl(null))
            .withSession(session
              + (Facebook.keys.originatingCall -> callFromRequest.toString)
              + (Facebook.keys.accessDeniedCall -> accessDeniedAction.toString))
        }, Input.Empty)
      }
    }

    Action(authenticatedBodyParser) { request =>
      val (innerAction, innerBody) = request.body
      innerAction(request.map(_ => innerBody))
    }

  }

}

object Facebook {
  object keys {
    //TODO move to utils project
    import play.api.Play.current
    private def playConfiguration(key: String)(implicit app:Application): Option[String] = app.configuration.getString(key)

    lazy val accessToken = playConfiguration("facebook.session.accesToken") getOrElse ("facebook.accessToken")
    lazy val originatingCall = playConfiguration("facebook.session.originatingCall") getOrElse ("facebook.originatingCall")
    lazy val accessDeniedCall = playConfiguration("facebook.session.accessDenied") getOrElse ("facebook.accessDenied")

    //TODO add sensible error message
    lazy val appId = playConfiguration("facebook.appId").getOrElse(throw PlayException("Configuration error", "Could not find facebook.appId in settings"))
    lazy val appSecret = playConfiguration("facebook.appSecret").getOrElse(throw PlayException("Configuration error", "Could not find facebook.appSecret in settings"))

  }
}

trait FacebookObject {

  var jsObject: Option[JsObject] = None

  def get[T](key: String)(implicit fjs: Reads[T]): T = {
    (jsObject.get \ key).as[T]
  }

  var accessToken: Option[String] = None
  lazy val verifier = new Verifier(accessToken.get)

  //needed by the field apply method
  implicit protected val f = this
}

trait FacebookObjectInformation[T <: FacebookObject] {
  val fields: List[Field]
  val scopePrefix: String

  def scopes = {
    fields
      .collect { case Field(permission, field) => permission }
      .distinct
      .map { scopePrefix + "_" + _.name }
      .mkString(",") match {
        case "" => None
        case s => Some(s)
      }
  }
}

case class Field(val permission: Permission, val name: String) {
  def apply[T]()(implicit f: FacebookObject, fjs: Reads[T]): T = f.get[T](name)
}

