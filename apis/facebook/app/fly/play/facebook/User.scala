package fly.play.facebook.user

import org.scribe.model.Verifier
import play.api.libs.json.Reads
import play.api.libs.json.JsObject
import fly.play.facebook.FacebookObject
import fly.play.facebook.DefaultUserPermission

//TODO set the correct access modifiers for the fields
abstract class AbstractUser(val id: Option[String]) extends FacebookObject {
  def this() = this(None)

  val permissionPrefix = "user"
  
 


}

abstract class AbstractFriend extends AbstractUser {
  override val permissionPrefix = "friend"
}

trait Name { self: AbstractUser =>
  private val field = << (DefaultUserPermission, "name")
  
  lazy val name = field[String]
}

trait FirstName { self: AbstractUser =>
  private val field = << (DefaultUserPermission, "first_name")

  lazy val firstName = field[String]
}

