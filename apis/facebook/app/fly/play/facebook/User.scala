package fly.play.facebook.user

import org.scribe.model.Verifier
import play.api.libs.json.Reads
import play.api.libs.json.JsObject
import fly.play.facebook.FacebookObject
import fly.play.facebook.Field
import scala.collection.mutable.ListBuffer
import fly.play.facebook.FacebookObjectInformation
import fly.play.facebook.FacebookObjectInformation
import fly.play.facebook.Permission
import fly.play.facebook.DefaultUserPermission

//TODO set the correct access modifiers for the fields
abstract class AbstractUser(val id: Option[String]) extends FacebookObject {
  def this() = this(None)
}

object AbstractUser {

  //Method to keep the amount of characters down
  def ->[T1: Manifest, T2: Manifest]: Boolean = {
    manifest[T2].erasure.isAssignableFrom(manifest[T2].erasure)
  }

  implicit def getFacebookObjectInformation[T <: AbstractUser: Manifest]: FacebookObjectInformation[T] = {

    import UserFields._

    val f = ListBuffer[Field]()

    if (->[Name, T]) 		f += name
    if (->[FirstName, T]) 	f += firstName
    if (->[MiddleName, T]) 	f += middleName
    if (->[LastName, T]) 	f += lastName
    if (->[Gender, T]) 		f += gender

    new FacebookObjectInformation[T] {
      val fields = f.toList
      val scopePrefix = if (->[AbstractFriend, T]) "friend" else "user"
    }
  }
}

abstract class AbstractFriend extends AbstractUser

object UserFields {
  val name = 		Field(DefaultUserPermission, "name")
  val firstName = 	Field(DefaultUserPermission, "first_name")
  val middleName = 	Field(DefaultUserPermission, "middle_name")
  val lastName = 	Field(DefaultUserPermission, "last_name")
  val gender = 		Field(DefaultUserPermission, "gender")
}

trait DefaultUser extends Name with FirstName with MiddleName with LastName { self: AbstractUser => }

trait Name { self: AbstractUser =>			lazy val name = UserFields.name[String] }
trait FirstName { self: AbstractUser => 	lazy val firstName = UserFields.firstName[String] }
trait MiddleName { self: AbstractUser => 	lazy val middleName = UserFields.middleName[String] }
trait LastName { self: AbstractUser =>  	lazy val lastName = UserFields.lastName[String] }
trait Gender { self: AbstractUser =>		lazy val gender = UserFields.gender[String] }

