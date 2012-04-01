package fly.play.utils
import play.api.mvc.Controller
import play.api.mvc.RequestHeader
import play.api.cache.Cache
import play.api.mvc.Action
import java.util.UUID
import play.api.mvc.SimpleResult
import play.api.mvc.PlainResult
import play.api.mvc.Session
import play.api.mvc.Result
import play.api.mvc.AsyncResult
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.mvc.Cookie
import play.api.mvc.BodyParser

object SessionCache {
  private val COOKIE_NAME = "sessionCacheId"
  
  def apply[A](action: CacheWrapper => Action[A]): Action[(Action[A], A, Option[Cookie])] = {
    val customValueBodyParser = BodyParser { request =>
      val (uuid, cookie) = request.cookies.get(COOKIE_NAME)
        .flatMap { c => Some(c.value) }
        .map {
          (_, None)
        }
        .getOrElse {
          val newUuid = UUID.randomUUID().toString()
          (newUuid, Some(Cookie(COOKIE_NAME, newUuid)))
        }

      val innerAction = action(new CacheWrapper(uuid))
      innerAction.parser(request).mapDone { body =>
        body.right.map(innerBody => (innerAction, innerBody, cookie))
      }
    }

    Action(customValueBodyParser) { request =>
      val (innerAction, innerBody, cookie) = request.body
      val result = innerAction(request.map(_ => innerBody))

      cookie.map { cookie =>
        addCookieToResult(cookie, result)
      }.getOrElse(result)
    }
  }

  private def addCookieToResult(cookie: Cookie, result: Result): Result = {
    result match {
      case r: AsyncResult => {
        AsyncResult(r.result.map { innerResult =>
          addCookieToResult(cookie, innerResult)
        })
      }
      case r: PlainResult => {
        r.withCookies(cookie)
      }
      case r => r
    }
  }
}