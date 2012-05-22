package fly.play.aws.auth

import play.api.libs.ws.WS
import play.api.http.Writeable
import play.api.http.ContentTypeOf
import java.net.URI
import fly.play.aws.Aws
import java.util.Date

case class Aws3Signer(credentials: AwsCredentials) extends Signer with SignerUtils {
  private val AwsCredentials(accessKeyId, secretKey, sessionToken, expirationSeconds) = credentials

  def sign(request: WS.WSRequestHolder, method: String): WS.WSRequestHolder =
    addAuthorizationHeaders(request, method, None, None)

  def sign[T](request: WS.WSRequestHolder, method: String, body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): WS.WSRequestHolder =
    addAuthorizationHeaders(request, method, Some(wrt transform body), ct.mimeType)

  private[auth] def addAuthorizationHeaders(request: WS.WSRequestHolder, method: String, body: Option[Array[Byte]], contentType: Option[String]): WS.WSRequestHolder = {

    import Aws.dates._

    val uri = URI.create(request.url)
    val host = uri.getHost
    val date = new Date
    val dateTime = rfc822DateFormat format date

    var newHeaders = addHeaders(request.headers, host, dateTime)

    val resourcePath = uri.getPath match {
      case "" | null => None
      case path => Some(path)
    }

    val (signedHeaders, cannonicalRequest) = createCannonicalRequest(method, resourcePath, request.queryString, newHeaders, body)

    val signature = base64Encode(sign(hash(cannonicalRequest), secretKey))

    val authorizationHeader = "AWS3 " +
      "AWSAccessKeyId=" + accessKeyId + "," +
      "Algorithm=HmacSHA256," +
      "SignedHeaders=" + signedHeaders + "," +
      "Signature=" + signature

    newHeaders += "X-Amzn-Authorization" -> Seq(authorizationHeader)

    request.copy(headers = newHeaders)
  }

  private[auth] def addHeaders(headers: Map[String, Seq[String]], host: String, dateTime: String): Map[String, Seq[String]] = {
    var newHeaders = headers

    sessionToken foreach (newHeaders += "X-Amz-Security-Token" -> Seq(_))

    val date = Seq(dateTime)

    newHeaders += "Host" -> Seq(host)
    newHeaders += "Date" -> date
    newHeaders += "X-Amz-Date" -> date

    newHeaders
  }

  private[auth] def createCannonicalRequest(method: String, resourcePath: Option[String], queryString: Map[String, String], headers: Map[String, Seq[String]], body: Option[Array[Byte]]): (String, String) = {

    val elligableHeaders = headers.keys.filter { k =>
      val lowerCaseKey = k.toLowerCase
      lowerCaseKey.startsWith("x-amz") || lowerCaseKey == "host"
    }

    val sortedHeaders = elligableHeaders.toSeq.sorted
    val signedHeaders = sortedHeaders.map(_.toLowerCase).mkString(";")

    val cannonicalRequest =
      method + "\n" +
        /* resourcePath */
        resourcePath.map(urlEncodePath _).getOrElse("/") + "\n" +
        /* queryString */
        queryString.toSeq.sorted.map { case (k, v) => urlEncode(k) + "=" + urlEncode(v) }.mkString("&") + "\n" +
        /* headers */
        sortedHeaders.map(k => k.toLowerCase + ":" + headers(k).mkString(" ") + "\n").mkString + "\n" +
        /* payload */
        (body.map(new String(_)) getOrElse "")

    (signedHeaders, cannonicalRequest)
  }
}