Play 2.0 module for AMF communication
=====================================

Small library module that allows you to use AMF to communicate with clients.

Note that this is developed with and for *Scala*.

Installation
------------

Add a resolver to your project settings:

``` scala
val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Releases Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-releases-local"
)
```

Add the dependency:

``` scala
	val appDependencies = Seq(
      "amf" %% "amf" % "1.0"
    )
```


Simple usage 
------------

From a Play perspective:

``` scala

	/* 
		routed with: POST   /echo   controllers.Application.echo
	*/
	def echo = Action(amfParser[Any]) { request =>
	  Amf(request.body)
	}

```

From an Actionscript perspective:

``` actionscript

	private function _clickHandler(e:MouseEvent):void
	{
		var a:AmfClient = new AmfClient();
		a.addEventListener(Event.COMPLETE, _completeHandler);
		a.request("http://localhost:9000/echo", {one: "value one", two: "value two"});
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
Most notably the **default (non-argument) constructor** and **vars** instead of vals

``` scala
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
```