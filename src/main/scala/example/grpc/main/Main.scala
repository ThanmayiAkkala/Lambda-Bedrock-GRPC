package example.grpc.main

import io.grpc.{Server, ServerBuilder, ManagedChannel, ManagedChannelBuilder}
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model._
import akka.stream.SystemMaterializer
import scala.util.{Failure, Success}
import org.slf4j.LoggerFactory
import example.grpc.service.QueryServiceGrpc
import example.grpc.service.QueryRequest
import example.grpc.service.QueryResponse


// Implementation of the QueryServicegg
class QueryServiceImpl()(implicit system: ActorSystem, ec: ExecutionContext) extends QueryServiceGrpc.QueryService {
  private val logger = LoggerFactory.getLogger(classOf[QueryServiceImpl])
  implicit val materializer = SystemMaterializer(system).materializer

  // Fetch API Gateway URL from environment variable or use a default
  protected val apiGatewayUrl = sys.env.getOrElse("API_GATEWAY_URL", "https://udqasoaqmk.execute-api.us-east-2.amazonaws.com/prod/query")

  override def triggerQuery(request: QueryRequest): Future[QueryResponse] = {
    logger.info("Received gRPC request with metadata (query): {}", request.metadata)

    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = apiGatewayUrl,
      entity = HttpEntity(ContentTypes.`application/json`, s"""{"query": "${request.metadata}"}""")
    )

    logger.info("Sending HTTP request to API Gateway: {}", httpRequest)

    // Implement retry logic with exponential backoff
    retryRequest(httpRequest, maxRetries = 3).map { body =>
      logger.info("Processed HTTP response body: {}", body)
      QueryResponse(result = body)
    }.recover {
      case ex: Exception =>
        logger.error("Error occurred during HTTP request or response processing: {}", ex.getMessage)
        QueryResponse(result = s"Error: ${ex.getMessage}")
    }
  }

  private def retryRequest(httpRequest: HttpRequest, maxRetries: Int, delay: Int = 1000): Future[String] = {
    Http().singleRequest(httpRequest).flatMap { response =>
      if (response.status.isSuccess()) {
        response.entity.dataBytes.runFold("")(_ + _.utf8String)
      } else {
        if (maxRetries > 0) {
          logger.warn("Request failed. Retrying in {}ms... (Retries left: {})", delay, maxRetries - 1)
          Thread.sleep(delay)
          retryRequest(httpRequest, maxRetries - 1, delay * 2)
        } else {
          Future.failed(new RuntimeException(s"Failed after multiple retries. Status: ${response.status}"))
        }
      }
    }
  }
}

object Main {
  private val logger = LoggerFactory.getLogger("Main")

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem("gRPCServerSystem",config)
    implicit val ec: ExecutionContext = system.dispatcher
    val logger = LoggerFactory.getLogger("gRPCClient")
    val port = sys.env.getOrElse("GRPC_PORT", "50051").toInt

    val server: Server = ServerBuilder
      .forPort(port)
      .addService(QueryServiceGrpc.bindService(new QueryServiceImpl()(system, ec), ec))
      .build()

    server.start()
    logger.info("gRPC Server started on port {}", port)

    sys.addShutdownHook {
      logger.info("Shutting down gRPC server...")
      server.shutdown()
      system.terminate()
      logger.info("ActorSystem terminated.")
    }

    runGrpcClient(port)

    server.awaitTermination()
  }

  def runGrpcClient(port: Int): Unit = {
    implicit val system: ActorSystem = ActorSystem("gRPCClientSystem")
    implicit val ec: ExecutionContext = system.dispatcher
    val logger = LoggerFactory.getLogger("gRPCClient")

    val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build()

    val stub = QueryServiceGrpc.stub(channel)

    val query = sys.env.getOrElse("QUERY", "Why do cats purr?")
    logger.info("Sending gRPC request with query: {}", query)

    val responseFuture: Future[QueryResponse] = stub.triggerQuery(QueryRequest(metadata = query))

    responseFuture.onComplete {
      case Success(response) =>
        logger.info("Received gRPC response: {}", response.result)
      case Failure(exception) =>
        logger.error("Error occurred while calling gRPC service: {}", exception.getMessage)
    }

    // Allow time for the response to be logged before shutting downhh
    Thread.sleep(5000)
    channel.shutdownNow()
  }
}
