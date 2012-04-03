package fly.play.facebook.permissions

import fly.play.facebook.user.Name
import fly.play.facebook.user.FirstName

sealed abstract case class Permission(val name:Option[String])

object Permission {
}

object AccessTokenPermission 
	extends Permission(None) 

object LikesPermission 
	extends Permission(Some("likes"))

object AboutMePermission 
	extends Permission(Some("about_me"))

