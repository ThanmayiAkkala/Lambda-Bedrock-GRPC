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
