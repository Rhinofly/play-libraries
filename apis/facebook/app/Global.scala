import play.api.GlobalSettings
import play.api.mvc.RequestHeader
import play.api.mvc.Handler

object Global extends GlobalSettings {
	override def onRouteRequest(request:RequestHeader):Option[Handler] = {
	  super.onRouteRequest(request)
	}
	
}