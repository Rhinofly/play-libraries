package fly.play.facebook.permissions

import fly.play.facebook.user.Name
import fly.play.facebook.user.FirstName

sealed abstract case class Permission(val name:Option[String])

object Permission {
  implicit def permissionToOption[T <: Permission](p:T):Option[T] = Some(p)
}

object AccessTokenPermission 
	extends Permission(None) 

object LikesPermission 
	extends Permission(Some("likes"))

object AboutMePermission 
	extends Permission(Some("about_me"))

