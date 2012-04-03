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
import fly.play.utils.CacheWrapper
import fly.play.utils.oauth.{ OAuth2, Facebook => FacebookOauth }
import fly.play.utils.oauth.OAuth2Constants._

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

  private def callFromRequest(implicit requestHeader: RequestHeader) =
    Call(requestHeader.method, requestHeader.path + "?" + requestHeader.rawQueryString)

  private lazy val redirectUri = callWithoutUrlParams(routes.FacebookController.callback(None, None, None))

  implicit private val facebookOauth =
    OAuth2(FacebookOauth(Facebook.keys.appId, Facebook.keys.appSecret), redirectUri)

  def signRequest(service: OAuthService, code: String, request: OAuthRequest) = {
    service.signRequest(service.getAccessToken(null, new Verifier(code)), request)
  }

  def FacebookAuthenticated[T <: FacebookObject, A](action: Promise[T] => Action[A])(implicit f: FacebookObjectInformation[T], m: Manifest[T]): Action[(Action[(Action[A], A)], (Action[A], A), Option[Cookie])] //these signatures are confusing, haha 
  = {
    SessionCache { implicit sessionCache =>

      WrappedAction(action) { implicit request =>

        Logger.info("Creating instance of " + m.erasure)
        val facebookObject = m.erasure.newInstance.asInstanceOf[T]

        if (FacebookService shouldRequestCodeFor facebookObject) {
          val authorizationUrl = facebookOauth.getAuthorizationUrl(f.scopes)
          Logger.info("Redirecting to facebook for authorization with " + authorizationUrl)
          Left(Redirect(authorizationUrl)
            .withSession(session
              + (Facebook.keys.originatingCall -> callFromRequest.toString)
              + (Facebook.keys.accessDeniedCall -> accessDeniedAction.toString)))
        } else {
          Right(FacebookService fill facebookObject)
        }
      }
    }

  }

}

object FacebookService {

  type Scopes = Option[String]
  type AccessTokenCache = Map[Scopes, String]

  def shouldRequestCodeFor[T <: FacebookObject](facebookObject: T)(implicit request: RequestHeader, f: FacebookObjectInformation[T], s: CacheWrapper): Boolean = {

    //TODO remove (used for debugging)
    Logger.info("Needs access token: " + needsAccessToken(facebookObject))
    Logger.info("Has valid access token: " + hasValidAccessTokenFor(f.scopes))
    Logger.info("Code in session: " + request.session.get(Facebook.keys.code).isDefined)
    
	needsAccessToken(facebookObject) && 
	!hasValidAccessTokenFor(f.scopes) && 
	//in the process of retrieving an access token
	request.session.get(Facebook.keys.code).isEmpty
  }

  def hasValidAccessTokenFor(scopes: Option[String])(implicit sessionCache: CacheWrapper): Boolean =
    getAccessTokenFor(scopes).isDefined

  def getAccessTokenFor(scopes: Option[String])(implicit sessionCache: CacheWrapper): Option[String] =
    sessionCache
      .getAs[AccessTokenCache](Facebook.keys.accessToken)
      .flatMap(_.get(scopes))

  def storeAccessToken[T <: FacebookObject](accessToken: String)(implicit sessionCache: CacheWrapper, f: FacebookObjectInformation[T]) = {
    val accessTokenCache = sessionCache
    	.getOrElse[AccessTokenCache](Facebook.keys.accessToken) {
      Logger.info("Creating new access token store")
      Map[Scopes, String]()
    } + (f.scopes -> accessToken)
    
    Logger.info("storing accessTokenCache: " + accessTokenCache)
    
    sessionCache.set(Facebook.keys.accessToken, accessTokenCache)
  }

  def needsAccessToken[T <: FacebookObject](facebookObject: T)(implicit f: FacebookObjectInformation[T]) =
    facebookObject.pathRequiresAccessToken || f.scopesRequireAccessToken

  def getAccessTokenFromCode[T <: FacebookObject](implicit request: RequestHeader, facebookOauth: OAuth2, f: FacebookObjectInformation[T]): Promise[String] = {
    //use the available code to get an access token
    request.session.get(Facebook.keys.code).map { code =>
      facebookOauth.getAccessToken(code, f.scopes)
    }.getOrElse {
      throw new Exception("No code available in session, need this to obtain an accesstoken.")
    }
  }

  def getUrlFor[T <: FacebookObject](facebookObject: T)(implicit request: RequestHeader, f: FacebookObjectInformation[T], sessionCache: CacheWrapper, facebookOauth: OAuth2): Promise[String] = {
    Akka.future {
      var url = Facebook.url +
        facebookObject.path +
        "?fields=" + f.fields.map(_.name).mkString(",")

      if (needsAccessToken(facebookObject)) {
        val accessToken =
          getAccessTokenFor(f.scopes)
            .getOrElse {
        		val accessToken = getAccessTokenFromCode.value.get
        		storeAccessToken(accessToken)
        		accessToken
            }

        

        url += "&" + ACCESS_TOKEN + "=" + accessToken
      }

      url
    }
  }

  def fill[T <: FacebookObject](facebookObject: T)(implicit request: RequestHeader, f: FacebookObjectInformation[T], sessionCache: CacheWrapper, facebookOauth: OAuth2): Promise[T] = {

    val url = getUrlFor(facebookObject)

    url.flatMap { url =>
      Logger.info("Calling facebook " + url)
      ws(url) { response =>
        facebookObject.jsValue = Some(response.json)

        facebookObject
      }
    }

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

  def path = id getOrElse (throw new Exception("Could not determine path for " + this))
  def pathRequiresAccessToken: Boolean
}

trait FacebookObjectInformation[T <: FacebookObject] {
  val fields: Seq[Field]
  val scopePrefix: String

  lazy val scopes: Option[String] = {
    //TODO can this be solved in a more elegant fasion?
    fields
      .collect { case Field(Some(Permission(Some(name))), _) => name }
      .distinct
      .map { scopePrefix + "_" + _ }
      .reduceOption(_ + "," + _) match {
        case s @ Some(_) => s
        case None => None
      }
  }

  lazy val scopesRequireAccessToken = scopes.isDefined
}

case class Field(val permission: Option[Permission], val name: String) {

  def this(permission: Permission, name: String) = this(Some(permission), name)

  def apply[T]()(implicit f: FacebookObject, fjs: Reads[T]): T = f.get[T](name)

}

object Field {
  def apply(permission: Permission, name: String): Field = Field(Some(permission), name)
}

class NamedObject(jsValue: Option[JsValue]) extends BasicObject(jsValue) {
  val id = get[String]("id")
  val name = get[String]("name")
}

object NamedObject {
  implicit object NameObjectReads extends Reads[NamedObject] {
    def reads(json: JsValue): NamedObject = new NamedObject(Some(json))
  }
}

object Facebook {

  lazy val url = "https://graph.facebook.com/"

  object keys {
    lazy val accessToken = playConfiguration("facebook.keys.session.accessToken") getOrElse ("facebook.accessToken")

    lazy val code = playConfiguration("facebook.keys.session.code") getOrElse ("facebook.code")
    lazy val originatingCall = playConfiguration("facebook.keys.session.originatingCall") getOrElse ("facebook.originatingCall")
    lazy val accessDeniedCall = playConfiguration("facebook.keys.session.accessDenied") getOrElse ("facebook.accessDenied")

    //TODO add sensible error message
    lazy val appId = playConfiguration("facebook.appId").getOrElse(throw PlayException("Configuration error", "Could not find facebook.appId in settings"))
    lazy val appSecret = playConfiguration("facebook.appSecret").getOrElse(throw PlayException("Configuration error", "Could not find facebook.appSecret in settings"))

  }
}