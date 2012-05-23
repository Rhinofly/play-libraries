package fly.play.aws.auth

import play.api.libs.ws.WS
import play.api.http.{ Writeable, ContentTypeOf }
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64

trait Signer {
  def sign(request: WS.WSRequestHolder, method: String): WS.WSRequestHolder
  def sign[T](request: WS.WSRequestHolder, method: String, body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): WS.WSRequestHolder
}

trait SignerUtils {
  val DEFAULT_ENCODING = "UTF-8"
  val EMPTY_HASH = hash("")
  
  def urlEncodePath(value: String) = urlEncode(value).replace("%2F", "/")

  def urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
    .replace("+", "%20")
    .replace("*", "%2A")
    .replace("%7E", "~")

  def toHex(b: Array[Byte]): String = b.map("%02x" format _).mkString

  def hash(str: String): Array[Byte] = hash(str getBytes DEFAULT_ENCODING)

  def hash(bytes: Array[Byte]): Array[Byte] = {
    val md = MessageDigest getInstance "SHA-256"
    md update bytes
    md digest
  }
  
  def sign(str: String, key: String): Array[Byte] = sign(str, key getBytes DEFAULT_ENCODING)

  def sign(str: String, key: Array[Byte]): Array[Byte] = sign(str getBytes DEFAULT_ENCODING, key)
  
  def sign(data: Array[Byte], key: String): Array[Byte] = sign(data, key getBytes DEFAULT_ENCODING)

  def sign(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val mac = Mac getInstance "HmacSHA256"
    mac init new SecretKeySpec(key, mac.getAlgorithm)
    mac doFinal data
  }
  
  def base64Encode(data:Array[Byte]):String = new String(Base64 encodeBase64 data)
}