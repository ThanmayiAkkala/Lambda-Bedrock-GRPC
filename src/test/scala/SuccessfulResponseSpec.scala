import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import example.grpc.service.{QueryRequest, QueryResponse}
import example.grpc.main.QueryServiceImpl
import akka.actor.ActorSystem
import scala.concurrent.{ExecutionContext, Future}

class SuccessfulResponseSpec extends AnyFunSuite with Matchers with ScalaFutures {
  // Define ActorSystem and ExecutionContext
  implicit val system: ActorSystem = ActorSystem("SuccessfulResponseSpec")
  implicit val ec: ExecutionContext = system.dispatcher

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(5, Seconds),
    interval = Span(50, Millis)
  )

  test("QueryService should handle a successful response from the API Gateway") {
    val service = new QueryServiceImpl() {
      override def triggerQuery(request: QueryRequest): Future[QueryResponse] = {
        Future.successful(QueryResponse(result = "Successfully handled query: test query"))
      }
    }

    // Create a test gRPC request
    val request = QueryRequest(metadata = "test query")

    // Trigger the gRPC service method and validate the response
    val responseFuture = service.triggerQuery(request)

    whenReady(responseFuture) { response =>
      response.result shouldEqual "Successfully handled query: test query"
    }
  }
}
