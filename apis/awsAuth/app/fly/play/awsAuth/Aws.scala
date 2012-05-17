package fly.play.awsAuth

import play.api.libs.ws.WS
import play.api.http.Writeable
import play.api.http.ContentTypeOf
import play.api.libs.concurrent.Promise
import play.api.libs.ws.Response
import play.api.libs.ws.ResponseHeaders
import play.api.libs.iteratee.Iteratee

object Aws {
  def url(url: String) = AwsRequestHolder(WS.url(url))

  case class AwsRequestHolder(wrappedRequest: WS.WSRequestHolder)(implicit signer: Signer) {
    def headers = wrappedRequest.headers
    def queryString = wrappedRequest.queryString

    def withHeaders(headers: (String, String)*): AwsRequestHolder = this.copy(wrappedRequest.withHeaders(headers: _*))
    def withQueryString(parameters: (String, String)*): AwsRequestHolder = this.copy(wrappedRequest.withHeaders(parameters: _*))

    val sign = signer.sign(wrappedRequest, _:String)
    def sign[T](method:String, body:T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]) = signer.sign(wrappedRequest, method, body)
    
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

  trait Signer {
    def sign(request: WS.WSRequestHolder, method: String): WS.WSRequestHolder
    def sign[T](request: WS.WSRequestHolder, method: String, body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): WS.WSRequestHolder
  }
}