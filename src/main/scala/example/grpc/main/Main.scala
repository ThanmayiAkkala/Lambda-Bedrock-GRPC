//// Main.scala
//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model._
//import akka.stream.SystemMaterializer
//import scala.concurrent.{Future, Await}
//import scala.concurrent.duration._
//
//object Main {
//  def main(args: Array[String]): Unit = {
//    implicit val system: ActorSystem = ActorSystem("SimpleAkkaClient")
//    implicit val materializer = SystemMaterializer(system).materializer
//    implicit val executionContext = system.dispatcher
//
//    val apiGatewayUrl = "https://udqasoaqmk.execute-api.us-east-2.amazonaws.com/prod/query"
//    val entity = HttpEntity(ContentTypes.`application/json`, """{}""")
//
//    val request = HttpRequest(HttpMethods.POST, uri = apiGatewayUrl, entity = entity)
//
//    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)
//
//    try {
//      val response = Await.result(responseFuture, 10.seconds)
//      println(s"Response status: ${response.status}")
//      response.entity.dataBytes.runFold("")(_ + _.utf8String).foreach { body =>
//        println(s"Response body: $body")
//      }
//    } catch {
//      case e: Exception =>
//        println(s"Request failed: $e")
//    } finally {
//      system.terminate()
//    }
//  }
//}
//package example.grpc
//
//import io.grpc.{ManagedChannel, ManagedChannelBuilder}
//import example.grpc.service.QueryServiceGrpc
//import example.grpc.service.QueryRequest
//import scala.concurrent.Await
//import scala.concurrent.duration._
//
//object Main {
//  def main(args: Array[String]): Unit = {
//    // Configure gRPC channel to point to the API Gateway URL
//    val apiGatewayUrl = "udqasoaqmk.execute-api.us-east-2.amazonaws.com" // Replace with your API Gateway URL
//    val channel: ManagedChannel = ManagedChannelBuilder.forAddress(apiGatewayUrl, 443)
//      .usePlaintext() // Use plaintext for HTTP/1.1
//      .build()
//
//    try {
//      // Create a blocking stub for the service
//      val blockingStub = QueryServiceGrpc.blockingStub(channel)
//
//      // Create a request
//      val request = QueryRequest(metadata = "")
//
//      // Make a gRPC call
//      val response = blockingStub.triggerQuery(request)
//      println(s"Response from Lambda: ${response.result}")
//    } catch {
//      case e: Exception =>
//        println(s"Error occurred: ${e.getMessage}")
//    } finally {
//      // Properly shut down the channel
//      channel.shutdownNow()
//      channel.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)
//    }
//  }
//}

//package example.grpc.main
//import io.grpc.{Server, ServerBuilder, ManagedChannel, ManagedChannelBuilder}
//import scala.concurrent.{ExecutionContext, Future}
//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model._
//import akka.stream.SystemMaterializer
//import scala.util.{Failure, Success}
//import example.grpc.service.QueryServiceGrpc
//import example.grpc.service.QueryResponse
//import example.grpc.service.QueryRequest
//
//
//// Implementation of the QueryService
//class QueryServiceImpl()(implicit system: ActorSystem, ec: ExecutionContext) extends QueryServiceGrpc.QueryService {
//  implicit val materializer = SystemMaterializer(system).materializer
//
//  private val apiGatewayUrl = "https://udqasoaqmk.execute-api.us-east-2.amazonaws.com/prod/query"
//
//  override def triggerQuery(request: QueryRequest): Future[QueryResponse] = {
//    val httpRequest = HttpRequest(
//      method = HttpMethods.POST,
//      uri = apiGatewayUrl,
//      entity = HttpEntity(ContentTypes.`application/json`, request.metadata)
//    )
//
//    Http().singleRequest(httpRequest).flatMap { response =>
//      response.entity.dataBytes.runFold("")(_ + _.utf8String).map { body =>
//        QueryResponse(result = body)
//      }
//    }.recover {
//      case ex: Exception =>
//        QueryResponse(result = s"Error: ${ex.getMessage}")
//    }
//  }
//}
//
//object Main {
//  def main(args: Array[String]): Unit = {
//    // Create an ActorSystem for Akka HTTP
//    implicit val system: ActorSystem = ActorSystem("gRPCServerSystem")
//    implicit val ec: ExecutionContext = system.dispatcher
//
//    // Start the gRPC server
//    val server: Server = ServerBuilder
//      .forPort(50051)
//      .addService(QueryServiceGrpc.bindService(new QueryServiceImpl()(system, ec), ec))
//      .build()
//
//    server.start()
//    println("gRPC Server started on port 50051")
//
//    // Add shutdown hook for cleanup
//    sys.addShutdownHook {
//      println("Shutting down gRPC server...")
//      server.shutdown()
//    }
//
//    // Run the gRPC client
//    runGrpcClient(ec)
//
//    // Await server termination
//    server.awaitTermination()
//  }
//
//  def runGrpcClient(implicit ec: ExecutionContext): Unit = {
//    // Create a gRPC client channel
//    val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
//      .usePlaintext()
//      .build()
//
//    val stub = QueryServiceGrpc.stub(channel)
//
//    // Send a request and handle the response
//    val responseFuture: Future[QueryResponse] = stub.triggerQuery(QueryRequest(metadata = ""))
//
//    responseFuture.onComplete {
//      case Success(response) =>
//        println(s"Response from Lambda (via gRPC): ${response.result}")
//      case Failure(exception) =>
//        println(s"Error occurred: ${exception.getMessage}")
//    }
//
//    // Wait for the response to be printed
//    Thread.sleep(5000)
//    channel.shutdownNow()
//  }
//}
//package example.grpc.service
//
//import io.grpc.{Server, ServerBuilder, ManagedChannel, ManagedChannelBuilder}
//import scala.concurrent.{ExecutionContext, Future}
//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model._
//import akka.stream.SystemMaterializer
//import scala.util.{Failure, Success}
//import org.slf4j.LoggerFactory
//
//// Implementation of the QueryService
//class QueryServiceImpl()(implicit system: ActorSystem, ec: ExecutionContext) extends QueryServiceGrpc.QueryService {
//  private val logger = LoggerFactory.getLogger(classOf[QueryServiceImpl])
//  implicit val materializer = SystemMaterializer(system).materializer
//
//  private val apiGatewayUrl = "https://udqasoaqmk.execute-api.us-east-2.amazonaws.com/prod/query"
//
//  override def triggerQuery(request: QueryRequest): Future[QueryResponse] = {
//    logger.info("Received gRPC request with metadata (query): {}", request.metadata)
//
//    // Build the HTTP request for the API Gateway
//    val httpRequest = HttpRequest(
//      method = HttpMethods.POST,
//      uri = apiGatewayUrl,
//      entity = HttpEntity(ContentTypes.`application/json`, s"""{"query": "${request.metadata}"}""") // Send query dynamically
//    )
//
//    logger.info("Sending HTTP request to API Gateway: {}", httpRequest)
//
//    // Make the HTTP request using Akka HTTP
//    Http().singleRequest(httpRequest).flatMap { response =>
//      logger.info("Received HTTP response with status: {}", response.status)
//
//      response.entity.dataBytes.runFold("")(_ + _.utf8String).map { body =>
//        logger.info("Processed HTTP response body: {}", body)
//        QueryResponse(result = body) // Return the response body
//      }
//    }.recover {
//      case ex: Exception =>
//        logger.error("Error occurred during HTTP request or response processing: {}", ex.getMessage)
//        QueryResponse(result = s"Error: ${ex.getMessage}")
//    }
//  }
//}
//
//object Main {
//  private val logger = LoggerFactory.getLogger("Main")
//
//  def main(args: Array[String]): Unit = {
//    // Create an ActorSystem for Akka HTTP
//    implicit val system: ActorSystem = ActorSystem("gRPCServerSystem")
//    implicit val ec: ExecutionContext = system.dispatcher
//
//    // Start the gRPC server
//    val server: Server = ServerBuilder
//      .forPort(50051)
//      .addService(QueryServiceGrpc.bindService(new QueryServiceImpl()(system, ec), ec))
//      .build()
//
//    server.start()
//    logger.info("gRPC Server started on port 50051")
//
//    // Add shutdown hook for cleanup
//    sys.addShutdownHook {
//      logger.info("Shutting down gRPC server...")
//      server.shutdown()
//    }
//
//    // Run the gRPC client
//    runGrpcClient()
//
//    // Await server termination
//    server.awaitTermination()
//  }
//
//  def runGrpcClient(): Unit = {
//    implicit val system: ActorSystem = ActorSystem("gRPCClientSystem")
//    implicit val ec: ExecutionContext = system.dispatcher
//    val logger = LoggerFactory.getLogger("gRPCClient")
//
//    val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
//      .usePlaintext()
//      .build()
//
//    val stub = QueryServiceGrpc.stub(channel)
//
//    // Send a request with a dynamic query
//    val query = "Why do cats purr?" // Replace with your dynamic query
//    logger.info("Sending gRPC request with query: {}", query)
//
//    val responseFuture: Future[QueryResponse] = stub.triggerQuery(QueryRequest(metadata = query))
//
//    responseFuture.onComplete {
//      case Success(response) =>
//        logger.info("Received gRPC response: {}", response.result)
//      case Failure(exception) =>
//        logger.error("Error occurred while calling gRPC service: {}", exception.getMessage)
//    }
//
//    // Wait for the response to be printed
//    Thread.sleep(5000)
//    channel.shutdownNow()
//  }
//}
package example.grpc.main

import io.grpc.{Server, ServerBuilder, ManagedChannel, ManagedChannelBuilder}
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.SystemMaterializer
import scala.util.{Failure, Success}
import org.slf4j.LoggerFactory
import example.grpc.service.QueryServiceGrpc
import example.grpc.service.QueryRequest
import example.grpc.service.QueryResponse

// Implementation of the QueryService
class QueryServiceImpl()(implicit system: ActorSystem, ec: ExecutionContext) extends QueryServiceGrpc.QueryService {
  private val logger = LoggerFactory.getLogger(classOf[QueryServiceImpl])
  implicit val materializer = SystemMaterializer(system).materializer

  // Fetch API Gateway URL from environment variable or use a default
  private val apiGatewayUrl = sys.env.getOrElse("API_GATEWAY_URL", "https://udqasoaqmk.execute-api.us-east-2.amazonaws.com/prod/query")

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
    implicit val system: ActorSystem = ActorSystem("gRPCServerSystem")
    implicit val ec: ExecutionContext = system.dispatcher

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

    // Allow time for the response to be logged before shutting down
    Thread.sleep(5000)
    channel.shutdownNow()
  }
}
