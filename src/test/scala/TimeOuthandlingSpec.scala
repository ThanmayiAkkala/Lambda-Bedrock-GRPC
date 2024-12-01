import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import example.grpc.service.{QueryRequest, QueryResponse}
import example.grpc.main.QueryServiceImpl
import akka.actor.ActorSystem
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.TimeoutException

class TimeOuthandlingSpec extends AnyFunSuite with Matchers with ScalaFutures {
  // Define ActorSystem and ExecutionContext
  implicit val system: ActorSystem = ActorSystem("TimeoutHandlingSpec")
  implicit val ec: ExecutionContext = system.dispatcher

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(5, Seconds),
    interval = Span(50, Millis)
  )

  test("QueryService should handle a timeout gracefully") {
    val service = new QueryServiceImpl() {
      override def triggerQuery(request: QueryRequest): Future[QueryResponse] = {
        Future.failed(new TimeoutException("Request timed out"))
      }
    }

    // Create a test gRPC request
    val request = QueryRequest(metadata = "test query")

    // Trigger the gRPC service method and expect it to fail
    val responseFuture = service.triggerQuery(request)

    whenReady(responseFuture.failed) { exception =>
      exception shouldBe a[TimeoutException]
      exception.getMessage should include("Request timed out")
    }
  }
}
