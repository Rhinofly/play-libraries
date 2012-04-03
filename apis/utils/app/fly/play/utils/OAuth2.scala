package fly.play.utils.oauth
import play.api.libs.concurrent.Promise
import java.net.URLEncoder
import play.api.mvc.Codec
import fly.play.utils.PlayUtils._
import java.net.URLDecoder
import play.api.mvc.Call
import play.api.mvc.RequestHeader
import OAuth2Constants._
import play.api.Logger

case class OAuth2(
  val api: OAuth2Api,
  val redirectUri: Call) {

  private val baseAccessTokenUrl =
    api.accessTokenEndPointUrl +
      "?" + CLIENT_ID + "=" + api.clientId +
  "&" + CLIENT_SECRET + "=" + api.clientSecret +
  "&" + REDIRECT_URI + "="

  //TODO include expires
  def getAccessToken(code: String, scope: Option[String] = None)(implicit codec: Codec, request: RequestHeader): Promise[String] = {
    val url = getAccessTokenUrl(code, scope)
    Logger.info("Requesting access token with url: " + url)
    ws(url) { response =>
      val AccessTokenResponse(accessToken, expires) = response.body
      URLDecoder.decode(accessToken, codec.charset)
    }
  }

  def getAccessTokenUrl(code: String, scope: Option[String])(implicit codec: Codec, request: RequestHeader): String =
    baseAccessTokenUrl + URLEncoder.encode(redirectUri.absoluteURL(), codec.charset) +
      "&" + CODE + "=" + code +
      scope.map("&" + SCOPE + "=" + _).getOrElse("")

  def getAuthorizationUrl(scope: Option[String])(implicit request: RequestHeader): String =
    api.getAuthorizationUrl(redirectUri, scope)
}

object OAuth2Constants {
  val CLIENT_ID = "client_id"
  val CLIENT_SECRET = "client_secret"
  val ACCESS_TOKEN = "access_token"
  val REDIRECT_URI = "redirect_uri"
  val CODE = "code"
  val SCOPE = "code"
  val AccessTokenResponse = """access_token=([^&]+)&expires=([^&]+)""".r
}

trait OAuth2Api {
  val clientId: String
  val clientSecret: String
  val accessTokenEndPointUrl: String

  def getAuthorizationUrl(redirectUri: Call, scope: Option[String])(implicit codec: Codec, request: RequestHeader): String

}

case class Facebook(val clientId: String, val clientSecret: String) extends OAuth2Api {
  val accessTokenEndPointUrl =
    "https://graph.facebook.com/oauth/access_token"

  private val baseAuthorizationUrl =
    "https://www.facebook.com/dialog/oauth" +
      "?" + CLIENT_ID + "=" + clientId +
      "&" + REDIRECT_URI + "="

  def getAuthorizationUrl(redirectUri: Call, scope: Option[String])(implicit codec: Codec, request: RequestHeader): String =
    baseAuthorizationUrl + URLEncoder.encode(redirectUri.absoluteURL(), codec.charset) +
      scope.map("&" + SCOPE + "=" + _).getOrElse("")
}