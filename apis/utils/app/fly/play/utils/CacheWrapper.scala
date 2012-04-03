package fly.play.utils
import play.api.cache.Cache
import play.api.Application

class CacheWrapper(val prefix:String) {
  
  private val fullPrefix = prefix + "."
  
  def set(key: String, value: Any, expiration: Int = 0)(implicit app: Application) = 
    Cache.set(fullPrefix + key, value, expiration)

  def get(key: String)(implicit app: Application): Option[Any] = 
	  Cache.get(fullPrefix + key)
    
  def getOrElse[A](key: String, expiration: Int = 0)(orElse: => A)(implicit app: Application, m: ClassManifest[A]): A = 
    Cache.getOrElse[A](key, expiration)(orElse)

    //TODO file bug for play. Should be typed as Manifest instead of ClassManifest in case type aliases are used
  def getAs[T](key: String)(implicit app: Application, m: Manifest[T]): Option[T] =
    get(key).map { item =>
      if (m.erasure.isAssignableFrom(item.getClass)) Some(item.asInstanceOf[T]) else None
    }.getOrElse(None)
}