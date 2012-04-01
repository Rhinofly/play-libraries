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

  def getAs[T](key: String)(implicit app: Application, m: ClassManifest[T]): Option[T] = 
    Cache.getAs[T](key)
}