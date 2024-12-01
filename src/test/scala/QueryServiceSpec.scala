import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import example.grpc.service.{QueryRequest, QueryResponse}
import example.grpc.main.QueryServiceImpl

class QueryServiceSpec extends AnyFunSuite with Matchers with ScalatestRouteTest {
  implicit val ec = system.dispatcher // Use the `system` provided by ScalatestRouteTest

  test("QueryService should return a valid response for a valid request") {
    // Set up a test HTTP server to mock the API Gateway
    val route = post {
      path("query") {
        entity(as[String]) { _ =>
          complete("test result") // Return a plain string instead of JSON
        }
      }
    }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    val mockApiGatewayUrl = "http://localhost:8080/query"

    // Initialize the service with the mock API Gateway URL
    val service = new QueryServiceImpl() {
      override protected val apiGatewayUrl: String = mockApiGatewayUrl
    }

    // Create a test gRPC request
    val request = QueryRequest(metadata = "test query")

    // Trigger the gRPC service method
    val responseFuture: Future[QueryResponse] = service.triggerQuery(request)

    // Wait for the response and validate it
    val response = Await.result(responseFuture, 5.seconds)
    response.result shouldEqual "test result" // Match the mocked response

    // Cleanup the mock server
    Await.ready(bindingFuture.flatMap(_.unbind()), 5.seconds)
  }
}
