package fly.play.sessionCache
import org.specs2.Specification
import play.api.mvc.Results
import play.api.mvc.Action
import play.api.test.FakeRequest
import play.api.mvc.Call
import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers
import play.api.mvc.Cookie

object SessionCacheSpec extends Specification with Results {
  def is =
    "SessionCache" ^
      "without cookie" !
      {
	    var sessionPrefix:String = null
    
        val action = SessionCache { sessionCache =>
          Action {
            sessionPrefix = sessionCache.asInstanceOf[CacheWrapper].prefix
            Ok
          }
        }

        val parsedRequest = action.parser(FakeRequest()).run.value.get

        var cookieValue:String = null
        
        val cookieSet = 
        parsedRequest match {
          case Right(body) => {
            val f = FakeRequest().copy(body = body)
            val result = action(f)
            val cookies = Helpers cookies result

            (cookies get SessionCache.COOKIE_NAME) must beLike {
              case Some(s) => {
            	  cookieValue = s.value                
            	  s.value.size must be_>(0)
              }
            }
          }
        }
        
        cookieSet and cookieValue === sessionPrefix

      } ^
      "with cookie" !
      {
        var sessionPrefix:String = null
        
        val action = SessionCache { sessionCache =>
          Action { 
            sessionPrefix = sessionCache.asInstanceOf[CacheWrapper].prefix
            Ok
          }
        }
        
        val parsedRequest = action
        		.parser(FakeRequest() withCookies Cookie(SessionCache.COOKIE_NAME, "abc"))
        		.run.value.get

        val cookieNotSet = 
          parsedRequest match {
          case Right(body) => {
            val f = FakeRequest().copy(body = body)
            val result = action(f)
            val cookies = Helpers cookies result

            (cookies get SessionCache.COOKIE_NAME) must beLike {
              case None => ok
            }
          }
        }
        
        cookieNotSet and sessionPrefix === "abc"

      } ^ end
}