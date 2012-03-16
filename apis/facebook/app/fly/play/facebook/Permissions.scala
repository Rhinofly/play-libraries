package fly.play.facebook
import fly.play.facebook.user.Name
import fly.play.facebook.user.FirstName

abstract class Permission(val name:Option[String], val hasAllFields:FacebookObject => Boolean) {}

object DefaultUserPermission 
	extends Permission(None, _.isInstanceOf[Name with FirstName]) 