import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.models.OllamaResult
import io.github.ollama4j.utils.Options
import scala.concurrent.ExecutionContext

case class MockOllamaResult(response: String) {
  def getResponse: String = response
}

class OllamaModelSpec extends AnyFunSuite with Matchers with ScalaFutures {
  implicit val ec: ExecutionContext = ExecutionContext.global

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(5, Seconds),
    interval = Span(50, Millis)
  )

  // Mock class to simulate OllamaResult

  test("Ollama Model should generate a valid query for the next conversation") {
    // Mock Ollama API
    val ollamaApiMock = mock(classOf[OllamaAPI])

    // Create a mock OllamaResult
    val mockOllamaResult = mock(classOf[OllamaResult])
    when(mockOllamaResult.getResponse).thenReturn("What is the capital of France?")

    // Configure the mock OllamaAPI to return the mock OllamaResult
    when(ollamaApiMock.generate(anyString(), anyString(), anyBoolean(), any[Options]))
      .thenReturn(mockOllamaResult)

    // Use the mock API to simulate the conversation
    val initialResponse = "The Eiffel Tower is in Paris."
    val prompt = s"Based on this response, what should the next query be?\nResponse: $initialResponse"

    val result = ollamaApiMock.generate("llama3.2:1b", prompt, false, new Options(new java.util.HashMap()))

    // Validate the generated query
    result.getResponse shouldEqual "What is the capital of France?"
  }

}
