package example.grpc.main


import spray.json._

case class RequestBody(prompt: String, max_tokens: Int)
case class ResponseBody(message: String)

object JsonSupport extends DefaultJsonProtocol {
  implicit val requestFormat: RootJsonFormat[RequestBody] = jsonFormat2(RequestBody)
  implicit val responseFormat: RootJsonFormat[ResponseBody] = jsonFormat1(ResponseBody)
}

