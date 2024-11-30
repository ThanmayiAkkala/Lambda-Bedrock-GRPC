//package example.grpc.main
//
//import io.github.ollama4j.OllamaAPI
//import io.github.ollama4j.models.OllamaResult
//import io.github.ollama4j.utils.Options
//import org.slf4j.LoggerFactory
//
//object ConversationAgent {
//  private val logger = LoggerFactory.getLogger("SimpleOllamaClient")
//
//  def main(args: Array[String]): Unit = {
//    val ollamaHost = "http://localhost:11434" // Ensure this matches the running Ollama server
//    val ollamaModel = "llama3.2:1b" // Replace with your desired model
//    val ollamaTimeout = 500 // Timeout in seconds
//    val prompt = "What is the significance of cats blinking slowly?"
//
//    val ollamaAPI = new OllamaAPI(ollamaHost)
//    ollamaAPI.setRequestTimeoutSeconds(ollamaTimeout)
//
//    try {
//      // Create Options object with empty parameters
//      val options = new Options(new java.util.HashMap[String, Object]())
//
//      // Call the generate method with all required parameters
//      val result: OllamaResult = ollamaAPI.generate(
//        ollamaModel, // Model name
//        prompt, // Input prompt
//        false, // Streaming flag
//        options // Options object
//      )
//      logger.info(s"Input Prompt: $prompt")
//      logger.info(s"Response from Ollama: ${result.getResponse}")
//    } catch {
//      case e: Exception =>
//        logger.error("Error while communicating with Ollama:", e)
//    }
//  }
//}
//package example.grpc.main
//
//import io.github.ollama4j.OllamaAPI
//import io.github.ollama4j.models.OllamaResult
//import io.github.ollama4j.utils.Options
//import example.grpc.service.{QueryRequest, QueryResponse, QueryServiceGrpc}
//import io.grpc.ManagedChannelBuilder
//import org.slf4j.LoggerFactory
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.{Failure, Success}
//
//object ConversationAgent {
//  private val logger = LoggerFactory.getLogger("ConversationAgent")
//
//  def main(args: Array[String]): Unit = {
//    if (args.length < 1) {
//      println("Please provide an initial query as a command-line argument.")
//      System.exit(1)
//    }
//
//    val initialQuery = args(0)
//    runConversation(initialQuery)
//  }
//
//  def runConversation(initialQuery: String): Unit = {
//    val ollamaHost = "http://localhost:11434"
//    val ollamaModel = "llama3.2:1b" // Update the model as needed
//    val ollamaTimeout = 500
//
//    val ollamaAPI = new OllamaAPI(ollamaHost)
//    ollamaAPI.setRequestTimeoutSeconds(ollamaTimeout)
//
//    val grpcChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
//      .usePlaintext()
//      .build()
//
//    val grpcStub = QueryServiceGrpc.stub(grpcChannel)
//
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    var currentQuery = initialQuery
//    var continueConversation = true
//
//    while (continueConversation) {
//      // Step 1: Send the current query to the gRPC server
//      val grpcResponseFuture: Future[QueryResponse] = grpcStub.triggerQuery(QueryRequest(metadata = currentQuery))
//
//      grpcResponseFuture.onComplete {
//        case Success(grpcResponse) =>
//          logger.info("Received response from Lambda (via gRPC): {}", grpcResponse.result)
//
//          // Step 2: Use Ollama to generate the next query
//          val nextQuery = generateNextQuery(ollamaAPI, grpcResponse.result, ollamaModel)
//          logger.info("Generated next query using Ollama: {}", nextQuery)
//
//          // Check termination condition
//          if (terminationCondition(nextQuery)) {
//            logger.info("Termination condition met. Ending conversation.")
//            continueConversation = false
//          } else {
//            currentQuery = nextQuery
//          }
//
//        case Failure(exception) =>
//          logger.error("Error during gRPC call:", exception)
//          continueConversation = false
//      }
//
//      // Add a small delay to prevent overwhelming the server
//      Thread.sleep(2000)
//    }
//
//    // Clean up gRPC channel
//    //grpcChannel.shutdownNow()
//  }
//
//  def generateNextQuery(ollamaAPI: OllamaAPI, grpcResponse: String, model: String): String = {
//    val ollamaPrompt = s"Based on the following response, what should the next query be?\nResponse: $grpcResponse"
//    try {
//      val options = new Options(new java.util.HashMap[String, Object]())
//      val ollamaResult: OllamaResult = ollamaAPI.generate(
//        model,
//        ollamaPrompt, false,
//        options
//      )
//      logger.info("Ollama generated response: {}", ollamaResult.getResponse)
//      ollamaResult.getResponse
//    } catch {
//      case e: Exception =>
//        logger.error("Error while generating next query using Ollama:", e)
//        "END"
//    }
//  }
//
//  def terminationCondition(query: String): Boolean = {
//    // Define termination condition, e.g., check for the keyword "END"
//    query.trim.equalsIgnoreCase("END")
//  }
//}
package example.grpc.main
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.models.OllamaResult
import io.github.ollama4j.utils.Options
import example.grpc.service.{QueryRequest, QueryResponse, QueryServiceGrpc}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object ConversationAgent {
  private val logger = LoggerFactory.getLogger("ConversationAgent")

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      println("Please provide an initial query as a command-line argument.")
      System.exit(1)
    }

    val initialQuery = args.mkString(" ")
    runConversation(initialQuery)
  }

  def runConversation(initialQuery: String): Unit = {
    val ollamaHost = "http://localhost:11434"
    val ollamaModel = "llama3.2:1b"
    val ollamaTimeout = 500

    val ollamaAPI = new OllamaAPI(ollamaHost)
    ollamaAPI.setRequestTimeoutSeconds(ollamaTimeout)

    val grpcChannel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
      .usePlaintext()
      .build()

    val grpcStub = QueryServiceGrpc.stub(grpcChannel)

    implicit val ec: ExecutionContext = ExecutionContext.global
    var currentQuery = initialQuery
    var continueConversation = true

    try {
      while (continueConversation) {
        val grpcResponse = Try {
          Await.result(grpcStub.triggerQuery(QueryRequest(metadata = currentQuery)), 10.seconds)
        } match {
          case Success(response) =>
            logger.info("Received response: {}", response.result)
            response.result
          case Failure(exception) =>
            logger.error("gRPC call failed:", exception)
            continueConversation = false
            ""
        }

        val nextQuery = Try {
          val prompt = s"Based on this response, what should the next query be?\nResponse: $grpcResponse"
          val options = new Options(new java.util.HashMap[String, Object]())
          ollamaAPI.generate(ollamaModel, prompt, false, options).getResponse.asInstanceOf[String]
        } match {
          case Success(query) =>
            logger.info("Generated query: {}", query)
            query
          case Failure(exception) =>
            logger.error("Ollama query failed:", exception)
            "END"
        }

        if (nextQuery.trim.equalsIgnoreCase("END")) {
          continueConversation = false
        } else {
          currentQuery = nextQuery
        }

        Thread.sleep(2000)
      }
    } finally {
      grpcChannel.shutdownNow()
    }
  }
}
//package example.grpc.main
//import io.grpc.{ManagedChannel, ManagedChannelBuilder}
//import io.github.ollama4j.OllamaAPI
//import io.github.ollama4j.models.OllamaResult
//import io.github.ollama4j.utils.Options
//import example.grpc.service.{QueryRequest, QueryResponse, QueryServiceGrpc}
//import org.slf4j.LoggerFactory
//
//import scala.concurrent.{Await, ExecutionContext, Future}
//import scala.concurrent.duration._
//import scala.util.{Failure, Success, Try}
//object ConversationAgent {
//  private val logger = LoggerFactory.getLogger("ConversationAgent")
//
//  def main(args: Array[String]): Unit = {
//    if (args.length < 1) {
//      println("Please provide an initial query as a command-line argument.")
//      System.exit(1)
//    }
//
//    val initialQuery = args(0)
//    runConversation(initialQuery)
//  }
//
//  def runConversation(initialQuery: String): Unit = {
//    val ollamaHost = "http://localhost:11434"
//    val ollamaModel = "llama3.2:1b"
//    val ollamaTimeout = 500
//
//    val ollamaAPI = new OllamaAPI(ollamaHost)
//    ollamaAPI.setRequestTimeoutSeconds(ollamaTimeout)
//
//    val grpcChannel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
//      .usePlaintext()
//      .build()
//
//    val grpcStub = QueryServiceGrpc.stub(grpcChannel)
//
//    implicit val ec: ExecutionContext = ExecutionContext.global
//    var currentQuery = initialQuery
//    var continueConversation = true
//
//    try {
//      while (continueConversation) {
//        val grpcResponse = Try {
//          Await.result(grpcStub.triggerQuery(QueryRequest(metadata = currentQuery)), 10.seconds)
//        } match {
//          case Success(response) =>
//            logger.info("Received response: {}", response.result)
//            response.result
//          case Failure(exception) =>
//            logger.error("gRPC call failed:", exception)
//            continueConversation = false
//            ""
//        }
//
//        val sanitizedQuery = generateNextQuery(ollamaAPI, grpcResponse, ollamaModel)
//        logger.info("Sanitized query: {}", sanitizedQuery)
//
//        if (terminationCondition(sanitizedQuery)) {
//          continueConversation = false
//        } else {
//          currentQuery = sanitizedQuery
//        }
//
//        Thread.sleep(2000)
//      }
//    } finally {
//      grpcChannel.shutdownNow() // Ensure the gRPC channel is properly closed
//    }
//  }
//
//
//  def generateNextQuery(ollamaAPI: OllamaAPI, grpcResponse: String, model: String): String = {
//    val ollamaPrompt = s"Based on this response, what should the next query be?\nResponse: $grpcResponse"
//    try {
//      val options = new Options(new java.util.HashMap[String, Object]())
//      val ollamaResult: OllamaResult = ollamaAPI.generate(model, ollamaPrompt, false, options)
//      val rawResponse = ollamaResult.getResponse
//      logger.info("Ollama generated response: {}", rawResponse)
//      sanitizeQuery(rawResponse)
//    } catch {
//      case e: Exception =>
//        logger.error("Error while generating next query using Ollama:", e)
//        "END" // Fallback to termination
//    }
//  }
//
//  def terminationCondition(query: String): Boolean = {
//    query.trim.equalsIgnoreCase("END")
//  }
//
//  def sanitizeQuery(query: String): String = {
//    query.replaceAll("\\p{C}", "").trim // Remove control characters
//  }
//}
