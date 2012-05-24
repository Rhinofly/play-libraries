package fly.play.utils

import play.api.Application
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.concurrent.Promise
import play.api.mvc.Call
import play.api.PlayException

object PlayUtils {

  def ws[T](url: String)(handler: Response => T): Promise[T] =
    WS.url(url).get().map(handler(_))

  //TODO create play ticket. Reverse url can not handle None
  def callWithoutUrlParams(call: Call): Call = Call(call.method, call.url.split('?')(0))

}