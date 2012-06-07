package fly.amf
{
    import flash.net.URLRequest;
    import flash.net.URLRequestMethod;
    import flash.net.URLStream;
    import flash.utils.ByteArray;

	public class AmfClient extends URLStream
    {
        public function AmfClient()
        {
        }
        
		public function get(url:String):void
		{
			load(new URLRequest(url));
		}
		
        public function post(url:String, data:Object, headers:Array = null):void
        {
            var byteArray:ByteArray = new ByteArray();
            byteArray.writeObject(data);
            
            var urlRequest:URLRequest = new URLRequest(url);
            urlRequest.method = URLRequestMethod.POST;
            urlRequest.contentType = "application/x-amf";
            urlRequest.data = byteArray;
            
            if (headers != null)
            {
                    urlRequest.requestHeaders = headers;
            }
            
            load(urlRequest);
        }
    }
}