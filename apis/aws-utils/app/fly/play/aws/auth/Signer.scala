package fly.play.aws.auth

import play.api.libs.ws.WS
import play.api.http.{Writeable, ContentTypeOf}

trait Signer {
  def sign(request: WS.WSRequestHolder, method: String): WS.WSRequestHolder
  def sign[T](request: WS.WSRequestHolder, method: String, body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): WS.WSRequestHolder
}