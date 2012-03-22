package controllers

import play.api._
import play.api.mvc._
import fly.play.facebook.FacebookApi
import fly.play.facebook.user._
import fly.play.facebook.DefaultUserPermission

object Application extends Controller with FacebookApi {
  
  val accessDeniedAction = routes.Application.facebookAccessDenied
  
  case class FacebookUser() extends AbstractUser with Name {
  }
  
  def index = Action {
	  Ok("Action ")
  }
  
  def test = FacebookAuthenticated { facebookUser:FacebookUser =>
    Action {
    	Ok(facebookUser.toString)
    }
  }
  
  def facebookAccessDenied = Action {
    Ok("facebook access denied")
  }
}