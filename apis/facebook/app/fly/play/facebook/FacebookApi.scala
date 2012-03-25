package fly.play.facebook
import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.Play
import play.api.Logger
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import org.scribe.builder.ServiceBuilder
import play.Configuration
import org.scribe.builder.api.{ FacebookApi => ScribeFacebookApi }
import play.api.mvc.Call
import controllers.routes
import play.api.mvc.RequestHeader
import org.scribe.model.Verifier
import org.scribe.model.Token
import org.scribe.model.OAuthRequest
import org.scribe.model.Verb
import org.scribe.model.OAuthConstants
import org.scribe.oauth.OAuthService
import scala.collection.mutable.ListBuffer
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import scala.collection.mutable.HashMap
import play.api.Application
import play.api.PlayException
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent.Akka

trait FacebookApi { self: Controller =>

  val accessDeniedAction: Call

  //TODO create play ticket. Reverse url can not handle None
  private def callWithoutUrlParams(call: Call): Call = Call(call.method, call.url.split('?')(0))

  private def callFromRequest(implicit requestHeader: RequestHeader) =
    Call(requestHeader.method, requestHeader.path + "?" + requestHeader.rawQueryString)

  private def facebookServiceBuilder()(implicit requestHeader: RequestHeader) = new ServiceBuilder()
    .provider(classOf[ScribeFacebookApi])
    .apiKey(Facebook.keys.appId)
    .apiSecret(Facebook.keys.appSecret)
    .callback(callWithoutUrlParams(routes.FacebookController.callback(None, None, None)).absoluteURL())

  private def facebookService[T <: FacebookObject]()(implicit requestHeader: RequestHeader, f: FacebookObjectInformation[T], m: Manifest[T]) = {
    val builder = facebookServiceBuilder
    f.scopes.foreach(builder.scope(_))
    builder.build()
  }

  def signRequest(service: OAuthService, code: String, request: OAuthRequest) = {
    service.signRequest(service.getAccessToken(null, new Verifier(code)), request)
  }

  def FacebookAuthenticated[T <: FacebookObject, A](action: Promise[T] => Action[A])(implicit f: FacebookObjectInformation[T], m: Manifest[T]): Action[(Action[A], A)] = {

    val authenticatedBodyParser = BodyParser { implicit request =>

      session.get(Facebook.keys.code).map { code =>

        import play.api.Play.current

        val facebookObjectPromise = Akka.future {
          val service = facebookService[T]

          val facebookObject = manifest[T].erasure.newInstance.asInstanceOf[T]
          val url = Facebook.url +
            facebookObject.id.getOrElse(f.path) +
            "?fields=" + f.fields.map(_.name).mkString(",")

          Logger.info("Creating oauth request to " + url)

          val facebookRequest = new OAuthRequest(Verb.GET, url)

          Logger.info("signing request with " + code)

          signRequest(service, code, facebookRequest)

          Logger.info("calling facebook " + facebookRequest.getCompleteUrl)
          //TODO should we do this async?
          val response = facebookRequest.send

          facebookObject.jsValue = Some(Json.parse(response.getBody))

          facebookObject
        }
        val innerAction = action(facebookObjectPromise)
        innerAction.parser(request).mapDone { body =>
          body.right.map(innerBody => (innerAction, innerBody))
        }
      }.getOrElse {
        Done(Left {
          val authorizationUrl = facebookService[T].getAuthorizationUrl(null)
          Logger.info("Redirecting to facebook for authorization with " + authorizationUrl)
          Redirect(authorizationUrl)
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

  lazy val url = "https://graph.facebook.com/"

  object keys {
    //TODO move to utils project
    import play.api.Play.current
    private def playConfiguration(key: String)(implicit app: Application): Option[String] = app.configuration.getString(key)

    lazy val code = playConfiguration("facebook.session.code") getOrElse ("facebook.code")
    lazy val originatingCall = playConfiguration("facebook.session.originatingCall") getOrElse ("facebook.originatingCall")
    lazy val accessDeniedCall = playConfiguration("facebook.session.accessDenied") getOrElse ("facebook.accessDenied")

    //TODO add sensible error message
    lazy val appId = playConfiguration("facebook.appId").getOrElse(throw PlayException("Configuration error", "Could not find facebook.appId in settings"))
    lazy val appSecret = playConfiguration("facebook.appSecret").getOrElse(throw PlayException("Configuration error", "Could not find facebook.appSecret in settings"))

  }
}

abstract class FacebookObject(val id: Option[String]) {

  var jsValue: Option[JsValue] = None

  def get[T](key: String)(implicit fjs: Reads[T]): T = {
    (jsValue.get \ key).as[T]
  }

  //needed by the field apply method
  implicit protected val f = this

  override def toString = {
    this.getClass.getSimpleName + " => \n" + jsValue
  }
}

trait FacebookObjectInformation[T <: FacebookObject] {
  val fields: List[Field]
  val scopePrefix: String
  val path: String

  def scopes = {
    fields
      .collect { case Field(permission, field) => permission }
      .collect { case Permission(Some(name)) => name }
      .distinct
      .map { scopePrefix + "_" + _ }
      .mkString(",") match {
        case "" => None
        case s => Some(s)
      }
  }
}

case class Field(val permission: Permission, val name: String) {
  def apply[T]()(implicit f: FacebookObject, fjs: Reads[T]): T = f.get[T](name)
}

