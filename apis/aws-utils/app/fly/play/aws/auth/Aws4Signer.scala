package fly.play.aws.auth

import fly.play.aws.Aws
import fly.play.aws.ServiceAndRegion
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.SimpleTimeZone
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import play.api.http.ContentTypeOf
import play.api.http.Writeable
import play.api.libs.ws.WS
import scala.Array.canBuildFrom

case class Aws4Signer(credentials: AwsCredentials, service: Option[String] = None, region: Option[String] = None) extends Signer {

  val AwsCredentials(accessKeyId, secretKey, sessionToken, expirationSeconds) = credentials

  def serviceAndRegion(backup: String): (String, String) = {
    lazy val ServiceAndRegion(extractedService, extractedRegion) = ServiceAndRegion(backup)

    (service getOrElse extractedService, region getOrElse extractedRegion)
  }

  def sign(request: WS.WSRequestHolder, method: String): WS.WSRequestHolder =
    addAuthorizationHeaders(request, method, None, None)

  def sign[T](request: WS.WSRequestHolder, method: String, body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): WS.WSRequestHolder =
    addAuthorizationHeaders(request, method, Some(wrt transform body), ct.mimeType)

  def addAuthorizationHeaders(request: WS.WSRequestHolder, method: String, body: Option[Array[Byte]], contentType: Option[String]): WS.WSRequestHolder = {

    import Aws.dates._

    val uri = URI.create(request.url)
    val host = uri.getHost
    val date = new Date
    val dateTime = dateTimeFormat format date

    var newHeaders = addHeaders(request.headers, host, dateTime, contentType)

    val resourcePath = uri.getPath match {
      case "" | null => None
      case path => Some(path)
    }

    val (signedHeaders, cannonicalRequest) = createCannonicalRequest(method, resourcePath, request.queryString, newHeaders, body)

    val dateStamp = dateStampFormat format date
    val (service, region) = serviceAndRegion(backup = host)
    val scope = dateStamp + "/" + region + "/" + service + "/" + TERMINATOR

    val stringToSign = createStringToSign(dateTime, cannonicalRequest, scope)

    var key = sign(dateStamp, "AWS4" + secretKey)
    key = sign(region, key)
    key = sign(service, key)
    key = sign(TERMINATOR, key)

    val authorizationHeader = ALGORITHM + " " +
      "Credential=" + accessKeyId + "/" + scope + ", " +
      "SignedHeaders=" + signedHeaders + ", " +
      "Signature=" + toHex(sign(stringToSign, key))

    newHeaders += "Authorization" -> Seq(authorizationHeader)

    request.copy(headers = newHeaders)
  }

  //TODO make non API members package private

  def addHeaders(headers: Map[String, Seq[String]], host: String, dateTime: String, contentType: Option[String]): Map[String, Seq[String]] = {
    var newHeaders = headers

    sessionToken foreach (newHeaders += "X-Amz-Security-Token" -> Seq(_))
    contentType foreach (newHeaders += "Content-type" -> Seq(_))

    newHeaders += "Host" -> Seq(host)
    newHeaders += "X-Amz-Date" -> Seq(dateTime)

    newHeaders
  }

  def createCannonicalRequest(method: String, resourcePath: Option[String], queryString: Map[String, String], headers: Map[String, Seq[String]], body: Option[Array[Byte]]): (String, java.lang.String) = {

    val sortedHeaders = headers.keys.toSeq.sorted
    val signedHeaders = sortedHeaders.map(_.toLowerCase).mkString(";")

    val cannonicalRequest =
      method + "\n" +
        /* resourcePath */
        resourcePath.map(urlEncodePath _).getOrElse("/") + "\n" +
        /* queryString */
        queryString.toSeq.sorted.map { case (k, v) => urlEncode(k) + "=" + urlEncode(v) }.mkString("&") + "\n" +
        /* headers */
        sortedHeaders.map(k => k.toLowerCase + ":" + headers(k).mkString(" ") + "\n").mkString + "\n" +
        /* signed headers */
        signedHeaders + "\n" +
        /* payload */
        toHex(body.map(hash _) getOrElse EMPTY_HASH)

    (signedHeaders, cannonicalRequest)
  }

  def createStringToSign(dateTime: String, cannonicalRequest: String, scope: String): String =
    ALGORITHM + "\n" +
      dateTime + "\n" +
      scope + "\n" +
      toHex(hash(cannonicalRequest))

  val DEFAULT_ENCODING = "UTF-8"
  val ALGORITHM = "AWS4-HMAC-SHA256"
  val TERMINATOR = "aws4_request"
  val EMPTY_HASH = hash("")

  def hash(str: String): Array[Byte] = hash(str getBytes DEFAULT_ENCODING)

  def hash(bytes: Array[Byte]): Array[Byte] = {
    val md = MessageDigest getInstance "SHA-256"
    md update bytes
    md digest
  }

  def sign(str: String, key: String): Array[Byte] = sign(str, key.getBytes(DEFAULT_ENCODING))

  def sign(str: String, key: Array[Byte]): Array[Byte] = sign(str.getBytes(DEFAULT_ENCODING), key)

  def sign(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val mac = Mac getInstance "HmacSHA256"
    mac init new SecretKeySpec(key, mac.getAlgorithm)
    mac doFinal data
  }

  def toHex(b: Array[Byte]): String = b.map("%02x" format _).mkString

  def urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
    .replace("+", "%20")
    .replace("*", "%2A")
    .replace("%7E", "~")

  def urlEncodePath(value: String) = urlEncode(value).replace("%2F", "/")

}
