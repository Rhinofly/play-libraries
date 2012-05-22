package fly.play.aws

import play.api.libs.ws.WS
import play.api.http.Writeable
import play.api.http.ContentTypeOf
import play.api.libs.concurrent.Promise
import play.api.libs.ws.Response
import play.api.libs.ws.ResponseHeaders
import play.api.libs.iteratee.Iteratee
import fly.play.aws.auth.Aws4Signer
import fly.play.aws.auth.AwsCredentials
import java.util.SimpleTimeZone
import java.text.SimpleDateFormat
import fly.play.aws.auth.Signer
import play.api.libs.ws.SignatureCalculator
import java.util.Locale
import fly.play.aws.auth.Aws3Signer

/**
 * Amazon Web Services
 */
object Aws {

  object EnableRedirects extends SignatureCalculator {
    def sign(request:WS.WSRequest) = request setFollowRedirects true
  }
  
  def withSigner3(credentials: AwsCredentials): AwsRequestBuilder =
		  withSigner3(Some(credentials))
	
  def withSigner3(credentials: Option[AwsCredentials] = None): AwsRequestBuilder =
    withSigner(Aws3Signer(credentials getOrElse AwsCredentials.fromConfiguration))
		  
  def withSigner4(credentials: AwsCredentials): AwsRequestBuilder =
    withSigner4(Some(credentials))

  def withSigner4(credentials: Option[AwsCredentials] = None, service: Option[String] = None, region: Option[String] = None): AwsRequestBuilder =
    withSigner(Aws4Signer(credentials getOrElse AwsCredentials.fromConfiguration, service, region))

  def withSigner(signer: Signer) = AwsRequestBuilder(signer)

  case class AwsRequestBuilder(signer: Signer) {
    def url(url: String): AwsRequestHolder = AwsRequestHolder(WS.url(url).sign(EnableRedirects), signer)
  }

  case class AwsRequestHolder(wrappedRequest: WS.WSRequestHolder, signer: Signer) {
    def headers = wrappedRequest.headers
    def queryString = wrappedRequest.queryString

    def withHeaders(headers: (String, String)*): AwsRequestHolder =
      this.copy(wrappedRequest = wrappedRequest.withHeaders(headers: _*))

    def withQueryString(parameters: (String, String)*): AwsRequestHolder =
      this.copy(wrappedRequest = wrappedRequest.withQueryString(parameters: _*))

    val sign = signer.sign(wrappedRequest, _: String)
    def sign[T](method: String, body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]) =
      signer.sign(wrappedRequest, method, body)

    def get(): Promise[Response] =
      sign("GET").get

    def get[A](consumer: ResponseHeaders => Iteratee[Array[Byte], A]): Promise[Iteratee[Array[Byte], A]] =
      sign("GET").get(consumer)

    def post[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Promise[Response] = 
      sign("POST", body).post(body)

    def postAndRetrieveStream[A, T](body: T)(consumer: ResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Promise[Iteratee[Array[Byte], A]] =
      sign("POST", body).postAndRetrieveStream(body)(consumer)

    def put[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Promise[Response] =
      sign("PUT", body).put(body)

    def putAndRetrieveStream[A, T](body: T)(consumer: ResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Promise[Iteratee[Array[Byte], A]] =
      sign("PUT", body).putAndRetrieveStream(body)(consumer)

    def delete(): Promise[Response] =
      sign("DELETE").delete

    def head(): Promise[Response] =
      sign("HEAD").head

    def options(): Promise[Response] =
      sign("OPTIONS").options

  }

  object dates {
    lazy val timeZone = new SimpleTimeZone(0, "UTC")

    def dateFormat(format: String, locale:Locale = Locale.getDefault): SimpleDateFormat = {
      val df = new SimpleDateFormat(format, locale)
      df setTimeZone timeZone
      df
    }

    lazy val dateTimeFormat = dateFormat("yyyyMMdd'T'HHmmss'Z'")
    lazy val dateStampFormat = dateFormat("yyyyMMdd")
    										
    lazy val iso8601DateFormat = dateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    lazy val rfc822DateFormat = dateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

  }
}