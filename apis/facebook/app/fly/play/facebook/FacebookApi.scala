package fly.play.facebook

//TODO clean up imports
import play.api.mvc.Cookie
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
import org.scribe.model.Response
import org.scribe.model.Verb
import org.scribe.model.OAuthConstants
import org.scribe.oauth.OAuthService
import org.scribe.utils.StreamUtils
import scala.collection.mutable.ListBuffer
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import scala.collection.mutable.HashMap
import play.api.Application
import play.api.PlayException
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent.Akka
import permissions.Permission
import java.net.URL
import java.net.HttpURLConnection
import fly.play.utils.PlayUtils._
import play.api.Play.current
import play.api.libs.ws.WS
import fly.play.utils.SessionCache
import fly.play.utils.WrappedAction

//TODO create library in utils for oauth 2.0
/*https://graph.facebook.com/oauth/access_token?
    client_id=YOUR_APP_ID
   &redirect_uri=YOUR_REDIRECT_URI
   &client_secret=YOUR_APP_SECRET
   &code=CODE_GENERATED_BY_FACEBOOK
   */
//access_token=USER_ACESS_TOKEN&expires=NUMBER_OF_SECONDS_UNTIL_TOKEN_EXPIRES
//

//TODO add state for facebook oAuth
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

  private def facebookService[T <: FacebookObject](scopes: Option[String] = None)(implicit requestHeader: RequestHeader, f: FacebookObjectInformation[T], m: Manifest[T]) = {
    val builder = facebookServiceBuilder
    scopes.foreach(builder.scope(_))
    builder.build()
  }

  def signRequest(service: OAuthService, code: String, request: OAuthRequest) = {
    service.signRequest(service.getAccessToken(null, new Verifier(code)), request)
  }

  def FacebookAuthenticated[T <: FacebookObject, A](action: Promise[T] => Action[A])(implicit f: FacebookObjectInformation[T], m: Manifest[T]): Action[(Action[(Action[A], A)], (Action[A], A), Option[Cookie])] //these signatures are confusing, haha 
  = {
	SessionCache { sessionCache =>
	    val (requiresAuthentication, scopes) = f.scopes
	
	    WrappedAction(action) { implicit request =>
	      Logger.info("Creating instance of " + manifest[T].erasure)
	      val facebookObject = manifest[T].erasure.newInstance.asInstanceOf[T]

	      val id = facebookObject.id
	      
	      val url = Facebook.url +
	        id.getOrElse(f.path) +
	        "?fields=" + f.fields.map(_.name).mkString(",")

	      val result: Option[Promise[T]] = if (requiresAuthentication || id.isEmpty)
	
	        session.get(Facebook.keys.code).map { code =>
	
	          Akka.future {
	            val service = facebookService[T]()
	
	            Logger.info("Creating oauth request to " + url)
	
	            val facebookRequest = new OAuthRequest(Verb.GET, url)
	
	            Logger.info("Signing request with " + code)
	
	            signRequest(service, code, facebookRequest)
	
	            Logger.info("Calling facebook " + facebookRequest.getCompleteUrl)
	
	            val response = facebookRequest.send
	
	            facebookObject.jsValue = Some(Json.parse(response.getBody))
	
	            facebookObject
	          }
	        }
	      else
	        Some(
	          Akka.future {
	            Logger.info("Calling facebook " + url)
	           
	            ws(url) { response =>
	               facebookObject.jsValue = Some(response.json)
	
	               facebookObject
	            }
	          })
	          
	       result match {
	        case Some(facebookObjectPromise) => Right(facebookObjectPromise)
	        case None => {
	          val authorizationUrl = facebookService[T](scopes).getAuthorizationUrl(null)
	          Logger.info("Redirecting to facebook for authorization with " + authorizationUrl)
	          Left(Redirect(authorizationUrl)
	            .withSession(session
	              + (Facebook.keys.originatingCall -> callFromRequest.toString)
	              + (Facebook.keys.accessDeniedCall -> accessDeniedAction.toString)))
	        }
	      }
	    }
	    
	    
	   
	  
	}

  }

}

object Facebook {

  
  lazy val url = "https://graph.facebook.com/"

  object keys {

    lazy val code = playConfiguration("facebook.session.code") getOrElse ("facebook.code")
    lazy val originatingCall = playConfiguration("facebook.session.originatingCall") getOrElse ("facebook.originatingCall")
    lazy val accessDeniedCall = playConfiguration("facebook.session.accessDenied") getOrElse ("facebook.accessDenied")

    //TODO add sensible error message
    lazy val appId = playConfiguration("facebook.appId").getOrElse(throw PlayException("Configuration error", "Could not find facebook.appId in settings"))
    lazy val appSecret = playConfiguration("facebook.appSecret").getOrElse(throw PlayException("Configuration error", "Could not find facebook.appSecret in settings"))

  }
}

abstract class BasicObject(var jsValue: Option[JsValue]) {

  def this() = this(None)
  
  def get[T](key: String)(implicit fjs: Reads[T]): T = {
    (jsValue.get \ key).as[T]
  }
  
   override def toString = {
    this.getClass.getSimpleName + " => \n" + jsValue
  }
}

abstract class FacebookObject(val id: Option[String]) extends BasicObject {

  //needed by the field apply method
  implicit protected val f = this

}

trait FacebookObjectInformation[T <: FacebookObject] {
  val fields: List[Field]
  val scopePrefix: String
  val path: String
  val pathRequiresAccessToken: Boolean
  
  def scopes: (Boolean, Option[String]) = {
    //TODO can this be solved in a more elegant fasion?
    fields
      .collect { case Field(Some(Permission(Some(name))), _) => name }
      .distinct
      .map { scopePrefix + "_" + _ }
      .reduceOption(_ + "," + _) match {
        case x @ Some(_) => (true, x)
        case None => (false, None)
      }

  }
}

case class Field(val permission: Option[Permission], val name: String) {

  def this(permission: Permission, name: String) = this(Some(permission), name)

  def apply[T]()(implicit f: FacebookObject, fjs: Reads[T]): T = f.get[T](name)
  
}

class NamedObject(jsValue: Option[JsValue]) extends BasicObject(jsValue) {
  val id = get[String]("id")
  val name = get[String]("name")
}

object NamedObject {
  implicit object NameObjectReads extends Reads[NamedObject] {
    def reads(json: JsValue):NamedObject = new NamedObject(Some(json))
  }
}
