Play 2.0 module for AMF communication
=====================================

Small library module that allows you to use AMF to communicate with clients.

Note that this is developed with and for *Scala*.

Installation
------------

Add a resolver to your project settings:

``` scala
val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-releases-local"
)
```

Add the dependency:

``` scala
	val appDependencies = Seq(
      "nl.rhinofly" %% "amf" % "1.0"
    )
```


Simple usage 
------------

From a Play perspective:

``` scala
object Application extends Controller with Amf /* Note that we add Amf support to our controller */ {

	/* 
		routed with: POST   /echo   controllers.Application.echo
	*/
	def echo = Action(amfParser[Any]) { request =>
	  Amf(request.body)
	}
}
```

From an Actionscript perspective:

``` actionscript

	private function _clickHandler(e:MouseEvent):void
	{
		var a:AmfClient = new AmfClient();
		a.addEventListener(Event.COMPLETE, _completeHandler);
		a.post("http://localhost:9000/echo", {one: "value one", two: "value two"});
	}
	
	private function _completeHandler(e:Event):void
	{
		var o:Object = AmfClient(e.currentTarget).readObject();
		trace("{one: \"" + o.one + "\", two: \"" + o.two + "\"}"); 
	}

```

In your flash project you can use the lightweight `AmfClient` which can be found at `/app/assets/actionscript/amf.swc`

More complicated typed objects
------------------------------

These require you to define your case classes in a certain way (limitation of the underlying Java library). 
Most notably the **default (non-argument) constructor** and **vars** instead of **vals**

``` scala
//Object definitions

	case class LatLng(
	    @BeanProperty var latitude:Double = 0, 
	    @BeanProperty var longitude:Double = 0) {
	  
		def this() = this(0, 0)
	}
	
	case class Image(
	    @BeanProperty var latLng:LatLng = null,
	    @BeanProperty var url:String = null,
	    @BeanProperty var caption:String = null,
	    @BeanProperty var size:Size = null
	) {
	  def this() = this(null, null, null, null)
	}

//The above object can then be used like this:

	//called using POST
	def saveImage = Action(amfParser[Image]) { request =>
	  val Image(latLng, url, caption, size) = request.body
	  
	  //store the information
	  
	  Amf(true)
	}
	
	//called using GET
	def list = Action { request =>
		val images:Array[Image] = getImages
	    Amf(images)
  	}
```

In actionscript:

``` actionscript
public function saveImage(image:Image):void
{
	var amfClient:AmfClient = new AmfClient();
	amfClient.addEventListener(Event.COMPLETE, _saveCompleteHandler);
	amfClient.addEventListener(ProgressEvent.PROGRESS, _saveProgessHandler);
	amfClient.addEventListener(IOErrorEvent.IO_ERROR, _saveErrorHandler);
	amfClient.addEventListener(SecurityErrorEvent.SECURITY_ERROR, _saveErrorHandler);
	//Do not forget to register the Image class using the registerClassAlias method
	amfClient.post("http://url/saveImage", image);
}

public function list():void
{
	var amfClient:AmfClient = new AmfClient();
	amfClient.addEventListener(Event.COMPLETE, _listCompleteHandler);
	amfClient.addEventListener(ProgressEvent.PROGRESS, _listProgessHandler);
	amfClient.addEventListener(IOErrorEvent.IO_ERROR, _listErrorHandler);
	amfClient.addEventListener(SecurityErrorEvent.SECURITY_ERROR, _listErrorHandler);
	amfClient.get("http://url/list");
}

private function _listCompleteHandler(event:Event):void
{
	var collection:Array = AmfClient(event.currentTarget).readObject() as Array;
	//...
}
```