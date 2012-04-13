This module allows you to use AMF to communicate with clients.

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

In order to set it up you need to download the 'amf' Play project and run `play publish-local` in that directory. Then you can add a dependency in your projects `Build.scala` file:

``` scala
	val appDependencies = Seq(
      // Add your project dependencies here,
      "amf" %% "amf" % "1.0"
    )
```

In your flash project you can use the lightweight `AmfClient` which can be found at `/app/assets/actionscript/amf.swc`

