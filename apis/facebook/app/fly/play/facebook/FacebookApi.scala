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

trait FacebookApi { self: Controller =>

  val accessDeniedAction: Call

  private def callFromRequest(implicit requestHeader: RequestHeader) =
    Call(requestHeader.method, requestHeader.path + "?" + requestHeader.rawQueryString)

  private def facebookServiceBuilder()(implicit requestHeader: RequestHeader) = new ServiceBuilder()
    .provider(classOf[ScribeFacebookApi])
    .apiKey(Facebook.keys.apiKey)
    .apiSecret(Facebook.keys.apiSecret)
    .callback(routes.FacebookController.callback.absoluteURL())

  def FacebookAuthenticated[T <: FacebookObject : Manifest, A](action: T => Action[A]): Action[(Action[A], A)] = {

    def redirect = { implicit request: RequestHeader =>
      {

       // val scopes = facebookUser.scopes.map { scope => scope.name }
        //val x = scopes.mkString(",")

        Done(Left(
          Redirect(facebookServiceBuilder.build().getAuthorizationUrl(null))
            .withSession(session
              + (Facebook.keys.originatingCall -> callFromRequest.toString)
              + (Facebook.keys.accessDeniedCall -> accessDeniedAction.toString))), Input.Empty)

      }
    }

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
          //val scopes = facebookUser.scopes.map { scope => scope.name }

          //facebookServiceBuilder.scope(scopes mkString ",")

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
    private def playConfiguration(key: String): Option[String] = Play.maybeApplication map (_.configuration.getString(key)) flatMap (e => e)

    lazy val accessToken = playConfiguration("facebook.session.accesToken") getOrElse ("facebook.accessToken")
    lazy val originatingCall = playConfiguration("facebook.session.originatingCall") getOrElse ("facebook.originatingCall")
    lazy val accessDeniedCall = playConfiguration("facebook.session.accessDenied") getOrElse ("facebook.accessDenied")

    //TODO add sensible error message
    lazy val apiKey = playConfiguration("facebook.apiKey").get
    lazy val apiSecret = playConfiguration("facebook.apiSecret").get

  }
}

trait FacebookObject {
  
  var jsObject: Option[JsObject] = None

  def get[T](key: String)(implicit fjs: Reads[T]): T = {
    (jsObject.get \ key).as[T]
  }
  
  var accessToken: Option[String] = None
  lazy val verifier = new Verifier(accessToken.get)
  
  val permissionInfo = HashMap[Permission, ListBuffer[String]]()

  implicit val self = this
  
  class Field(val permission:Permission, val name:String) {
	def apply[T]()(implicit f:FacebookObject, fjs: Reads[T]):T = f.get[T](name)
  }

  def <<(permission:Permission, field:String):Field = {
	  permissionInfo getOrElseUpdate(permission, ListBuffer[String]()) += field
	  
	  new Field(permission, field)
  }
}

