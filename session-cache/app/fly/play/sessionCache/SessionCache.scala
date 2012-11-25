package fly.play.sessionCache
import play.api.mvc.Action
import play.api.mvc.Cookie
import play.api.mvc.BodyParser
import java.util.UUID
import play.api.mvc.Result
import play.api.mvc.AsyncResult
import play.api.mvc.PlainResult
import play.api.Application
import play.api.mvc.RequestHeader

/**
 * Essentially has the same interface as the regular built-in cache
 * 
 * For more information on retrieving an instance of the session cache, 
 * check the companion object
 */
trait SessionCache {
  
  /**
   * Sets a value without expiration
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration expiration period in seconds.
   */  
  def set(key: String, value: Any, expiration: Int = 0)(implicit app: Application)
  
  /**
   * Retrieve a value from the cache.
   *
   * @param key Item key.
   */  
  def get(key: String)(implicit app: Application): Option[Any]
  
  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period in seconds.
   * @param orElse The default function to invoke if the value was found in cache.
   */  
  def getOrElse[A](key: String, expiration: Int = 0)(orElse: => A)(implicit app: Application, m: ClassManifest[A]): A

  /**
   * Retrieve a value from the cache for the given type
   *
   * @param key Item key.
   * @return result as Option[T]
   */
  def getAs[T](key: String)(implicit app: Application, m: Manifest[T]): Option[T]
}

/**
 * Provides a wrapper for actions.
 * 
 * Usage:
 * {{{
 * def index = SessionCache { sessionCache =>
 *    Action {
 *       ...
 *    }
 * }
 * }}}
 * 
 * What the Action wrapper does is check for the existence of a cookie. If it is 
 * found the value of the cookie (a UUID) is used as a key in the regular cache. 
 * If it's not found a cookie is added to the result of the action. 
 */
object SessionCache {
  
  private[sessionCache] val COOKIE_NAME = "sessionCacheId"

  /**
   * Allows you to retrieve the SessionCache if you already had it created 
   * using the Action wrapper
   * 
   * @return session cache wrapped in option or none
   */
  def get()(implicit request: RequestHeader): Option[SessionCache] =
    request.cookies get COOKIE_NAME map (c => new CacheWrapper(c.value))

  /**
   * The actual action wrapper that allows you to execute an action which 
   * can use the session cache
   */
  def apply[A](action: SessionCache => Action[A]): Action[(Action[A], A, Option[Cookie])] = {

    //create a body parser with the inner action
    val customValueBodyParser = BodyParser { request =>

      //we need a pair of a uuid (user identifier) and a cookie for that uuid
      val (uuid, cookieOption) =
        request.cookies
          .get(COOKIE_NAME)
          .map(_.value -> None)
          .getOrElse {
            //no cookie found, create a uuid and prepare a cookie
            val newUuid = UUID.randomUUID.toString
            (newUuid, Some(Cookie(COOKIE_NAME, newUuid)))
          }

      val innerAction = action(new CacheWrapper(uuid))
      innerAction parser request mapDone { body =>
        body.right map ((innerAction, _, cookieOption))
      }
    }

    //The actual action, wraps the inner action
    Action(customValueBodyParser) { request =>
      val (innerAction, innerBody, cookieOption) = request.body
      //call the actual inner action
      val result = innerAction(request.map(_ => innerBody))

      cookieOption map (addCookieToResult(_, result)) getOrElse result
    }
  }

  private def addCookieToResult(cookie: Cookie, result: Result): Result =
    result match {
      case r: AsyncResult =>
        AsyncResult(r.result map (addCookieToResult(cookie, _)))

      case r: PlainResult => r.withCookies(cookie)

      case r => r
    }

}