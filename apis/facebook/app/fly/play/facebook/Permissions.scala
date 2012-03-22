package fly.play.facebook
import fly.play.facebook.user.Name
import fly.play.facebook.user.FirstName

abstract class Permission(val name:Option[String])

object DefaultUserPermission 
	extends Permission(None) 