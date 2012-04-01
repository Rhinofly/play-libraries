package controllers

import play.api._
import play.api.mvc._
import fly.play.facebook.FacebookApi
import fly.play.facebook.user._
import play.api.libs.concurrent.Promise

object Application extends Controller with FacebookApi {
  
  val accessDeniedAction = routes.Application.facebookAccessDenied
  
  lazy val id = {
    println("loading id")
    "709386937"
  }
  
  class FacebookUser extends AbstractUser/*(id)*/ with PublicUser {
  }
  
  def index = Action {
	  Ok("Action ")
  }
  
  def test = FacebookAuthenticated { facebookUserPromise:Promise[FacebookUser] =>
    Action {
      Async {
    	  facebookUserPromise.map { facebookUser =>
	    	  Ok(facebookUser.toString)
    	  }
      }
    }
  }
  
  def facebookAccessDenied = Action {
    Ok("facebook access denied")
  }
}