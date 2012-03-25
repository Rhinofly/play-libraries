package fly.play.facebook
import fly.play.facebook.user.Name
import fly.play.facebook.user.FirstName

sealed abstract case class Permission(val name:Option[String])

object DefaultUserPermission 
	extends Permission(None) 