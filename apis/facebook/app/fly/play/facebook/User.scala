package fly.play.facebook.user

import org.scribe.model.Verifier
import play.api.libs.json.Reads
import play.api.libs.json.JsObject
import fly.play.facebook.FacebookObject
import fly.play.facebook.Field
import scala.collection.mutable.ListBuffer
import fly.play.facebook.FacebookObjectInformation
import fly.play.facebook.FacebookObjectInformation
import fly.play.facebook.permissions._
import play.api.PlayException
import fly.play.facebook.NamedObject
import play.api.i18n.Lang
import play.api.libs.json.JsValue
import play.api.libs.json.JsString
import java.net.URL

//TODO set the correct access modifiers for the fields
abstract class AbstractUser(id:Option[String]) extends FacebookObject(id) {
  /**
   * Sets the id to 'me' (indicating the current user)
   */
  def this() = this(Some("me"))
  def this(id:String) = this(Some(id))
  
  def pathRequiresAccessToken = path == "me"
}

object AbstractUser {

  //Method to keep the amount of characters down
  def ->[T1: Manifest, T2: Manifest]: Boolean = {
    manifest[T1].erasure.isAssignableFrom(manifest[T2].erasure)
  }

  implicit def getFacebookObjectInformation[T <: AbstractUser: Manifest]: FacebookObjectInformation[T] = {

    //TODO should we implement caching?
    import UserFields._

    val f = ListBuffer[Field]()

    if (->[Name, T]) 			f += name
    if (->[FirstName, T]) 		f += firstName
    if (->[MiddleName, T]) 		f += middleName
    if (->[LastName, T]) 		f += lastName
    if (->[Gender, T]) 			f += gender
    if (->[Locale, T]) 			f += locale
    if (->[Link, T]) 			f += link
    if (->[Username, T]) 		f += username
    
    if (->[Timezone, T]) 		f += timezone
    if (->[ThirdPartyId, T]) 	f += username
    
    if (->[Languages, T]) 		f += languages

    if (->[Bio, T]) 			f += bio
    
    new FacebookObjectInformation[T] {
      val fields = f.toSeq
      val scopePrefix = if (->[AbstractFriend, T]) "friend" else "user"
    }
  }
}

object UserFields {
  
  val name = 			Field(None, "name")
  val firstName = 		Field(None, "first_name")
  val middleName = 		Field(None, "middle_name")
  val lastName = 		Field(None, "last_name")
  val gender = 			Field(None, "gender")
  val locale = 			Field(None, "locale")
  val link = 			Field(None, "link")
  val username = 		Field(None, "username")
  
  val timezone = 		Field(AccessTokenPermission, "timezone")
  val thirdPartyId = 	Field(AccessTokenPermission, "third_party_id")
  
  val languages = 		Field(LikesPermission, "languages") 
  
  val bio = 			Field(AboutMePermission, "bio")
}

object implicits {
    implicit object LangReads extends Reads[Lang] {
    	def reads(json: JsValue):Lang = Lang(Reads.StringReads.reads(json))
	}
    
    implicit object URLReads extends Reads[URL] {
    	def reads(json: JsValue):URL = new URL(Reads.StringReads.reads(json))
    }
}

abstract class AbstractFriend(id:Option[String]) extends AbstractUser(id) {
  def this() = this(None)
}

import implicits._

//TODO move to UserFields

trait PublicUser 
	extends Name with FirstName with MiddleName with LastName with Gender with Locale with Link with Username { self: AbstractUser => }

trait Name { self: AbstractUser =>			lazy val name = UserFields.name[String] }
trait FirstName { self: AbstractUser => 	lazy val firstName = UserFields.firstName[String] }
trait MiddleName { self: AbstractUser => 	lazy val middleName = UserFields.middleName[String] }
trait LastName { self: AbstractUser =>  	lazy val lastName = UserFields.lastName[String] }
trait Gender { self: AbstractUser =>		lazy val gender = UserFields.gender[String] }
trait Locale { self: AbstractUser =>		lazy val locale = UserFields.locale[Lang] }
trait Link { self: AbstractUser =>			lazy val link = UserFields.link[URL] }
trait Username { self: AbstractUser =>		lazy val username = UserFields.username[String] }

trait AccessTokenUser
	extends PublicUser with Timezone {self:AbstractUser => }

trait Timezone {self: AbstractUser => 		lazy val timezone = UserFields.timezone[Int] }
trait ThirdPartyId {self:AbstractUser =>	lazy val thirdPartyId = UserFields.thirdPartyId[String] }

trait LikesUser
	extends AccessTokenUser with Languages {self:AbstractUser => } 

trait Languages {self: AbstractUser => 		lazy val languages = UserFields.languages[Seq[NamedObject]] }

trait AboutMeUser
	extends AccessTokenUser with Bio {self:AbstractUser => }

//TODO, we should probably make this and other optional fields an Option
trait Bio { self:AbstractUser  =>		lazy val bio = UserFields.bio[String] }

