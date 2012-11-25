package fly.play.sessionCache
import play.api.cache.Cache
import play.api.Application

/**
 * Simply wraps the internal cache and uses the given prefix to prefix 
 * the key on every operation.
 * 
 * Usage:
 * 
 * new CacheWrapper("prefix").get("key") // Is the same as Cache.get("prefix.key")
 */
class CacheWrapper(val prefix: String) extends SessionCache {

  private val fullPrefix = prefix + "."

  override def set(key: String, value: Any, expiration: Int = 0)(implicit app: Application) =
    Cache.set(fullPrefix + key, value, expiration)

  override def get(key: String)(implicit app: Application): Option[Any] =
    Cache.get(fullPrefix + key)

  override def getOrElse[A](key: String, expiration: Int = 0)(orElse: => A)(implicit app: Application, m: ClassManifest[A]): A =
    Cache.getOrElse[A](fullPrefix + key, expiration)(orElse)

  //Can not use the cache method directly: See ticket #881 in lighthouse for Play 2
  override def getAs[T](key: String)(implicit app: Application, m: Manifest[T]): Option[T] =
    get(key)
      .flatMap { item =>
        if (m.erasure isAssignableFrom item.getClass) Some(item.asInstanceOf[T])
        else None
      }
}