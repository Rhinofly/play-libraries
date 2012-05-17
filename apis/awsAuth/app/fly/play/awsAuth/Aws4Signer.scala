package fly.play.awsAuth

import play.api.libs.ws.WS.WSRequestHolder
import java.text.SimpleDateFormat
import java.util.SimpleTimeZone
import java.util.Date
import scala.collection.mutable.StringBuilder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.text.DateFormat
import java.net.URLEncoder
import play.api.libs.ws.SignatureCalculator
import play.api.libs.ws.WS

class Aws4Signer(credentials: AwsCredentials, service: Option[String] = None, region: Option[String] = None) {

  val AwsCredentials(accessKeyId, secretKey, sessionToken) = credentials

  def serviceAndRegion(backup: String): (String, String) = {
    lazy val ServiceAndRegion(extractedService, extractedRegion) = ServiceAndRegion(backup)

    (service getOrElse extractedService, region getOrElse extractedRegion)
  }

  def addAuthorizationHeaders(headers: Map[String, String], host: String, method: String,
    resourcePath: Option[String], queryString: Map[String, String], content: Option[String]): Map[String, String] = {

    import util._

    var newHeaders = headers

    sessionToken.foreach(newHeaders += "x-amz-security-token" -> _)

    val date = new Date
    val dateTime = dateTimeFormat format date

    newHeaders += "Host" -> host
    newHeaders += "X-Amz-Date" -> dateTime

    val sortedHeaders = newHeaders.keys.toSeq.sorted
    val signedHeaders = sortedHeaders.map(_.toLowerCase).mkString(";")

    val cannonicalRequest =
      method + "\n" +
        /* resourcePath */
        resourcePath.map(urlEncodePath _).getOrElse("/") + "\n" +
        /* queryString */
        queryString.map { case (k, v) => urlEncode(k) + "=" + urlEncode(v) }.mkString("&") + "\n" +
        /* headers */
        sortedHeaders.map(k => k.toLowerCase + ":" + newHeaders(k) + "\n").mkString + "\n" +
        /* signed headers */
        signedHeaders + "\n" +
        /* payload */
        content.map(hash _ andThen toHex).getOrElse("")

    val dateStamp = dateStampFormat format date
    val (service, region) = serviceAndRegion(backup = host)
    val scope = dateStamp + "/" + region + "/" + service + "/" + TERMINATOR

    val stringToSign =
      ALGORITHM + "\n" +
        dateTime + "\n" +
        scope + "\n" +
        toHex(hash(cannonicalRequest))

    var key = sign(dateStamp, "AWS4" + secretKey)
    key = sign(region, key)
    key = sign(service, key)
    key = sign(TERMINATOR, key)

    val authorizationHeader = ALGORITHM + " " +
      "Credentials=" + accessKeyId + "/" + scope + ", " +
      "SignedHeaders=" + signedHeaders + ", " +
      "Signature=" + toHex(sign(stringToSign, key))

    newHeaders += "Authorization" -> authorizationHeader

    newHeaders
  }

  object util {
    val DEFAULT_ENCODING = "UTF-8"
    val ALGORITHM = "AWS4-HMAC-SHA256"
    val TERMINATOR = "aws4_request"

    def hash(str: String): Array[Byte] = {
      val md = MessageDigest getInstance "SHA-256"
      md update (str getBytes DEFAULT_ENCODING)
      md digest
    }

    def sign(str: String, key: String): Array[Byte] = sign(str, key.getBytes(DEFAULT_ENCODING))
    
    def sign(str: String, key: Array[Byte]): Array[Byte] = sign(str.getBytes(DEFAULT_ENCODING), key)

    def sign(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
      val mac = Mac getInstance "HmacSHA256"
      mac init new SecretKeySpec(key, mac.getAlgorithm)
      mac doFinal data
    }

    def toHex(b: Array[Byte]): String = b.map("%02X" format _).mkString

    def urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
      .replace("+", "%20")
      .replace("*", "%2A")
      .replace("%7E", "~")

    def urlEncodePath(value: String) = urlEncode(value).replace("%2F", "/")

    lazy val timeZone = new SimpleTimeZone(0, "UTC")

    def dateFormat(format: String): SimpleDateFormat = {
      val df = new SimpleDateFormat(format)
      df setTimeZone timeZone
      df
    }

    lazy val dateTimeFormat = dateFormat("yyyyMMdd'T'HHmmss'Z'")
    lazy val dateStampFormat = dateFormat("yyyyMMdd")
  }
}

