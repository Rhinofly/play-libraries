package fly.play.utils

import play.api.Application
import play.api.libs.ws.WS
import play.api.libs.ws.Response

object PlayUtils {

  def playConfiguration(key: String)(implicit app: Application): Option[String] =
    app.configuration.getString(key)

  def ws[T](url: String)(handler: Response => T): T =
    WS.url(url).get().map(handler(_)).value.get

    
}