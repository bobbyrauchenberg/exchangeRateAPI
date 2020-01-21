package com.rauchenberg.exchangeRateApi.server

import cats.effect.IO.{Map => _}
import cats.effect._
import cats.scalatest.{EitherMatchers, EitherValues}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.rauchenberg.exchangeRateApi.common.{UpstreamResponse, WireMockSpec}
import com.rauchenberg.exchangeRateApi.domain.{ConversionRequest, ConversionResult, Error}
import com.rauchenberg.exchangeRateApi.domain.Error
import io.circe.syntax._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{DecodeFailure, EntityDecoder, EntityEncoder, Method, Request, Uri}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

class ExchangeRateServerSpec extends AnyWordSpecLike with WireMockSpec with Matchers with EitherValues with EitherMatchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  val appPort = 8080

  val server =
    ExchangeRateServer
      .stream[IO](
        appPort,
        (from: String, to: String) => s"http://localhost:$port/latest?base=$from&symbols=$to", Option(1 second)
      ).compile.drain

  val fiber = server.start.unsafeRunSync()

  override def afterAll(): Unit = {
    wireMock.stop()
    fiber.cancel.unsafeRunSync()
    super.afterAll()
  }

  "an outbound http call" should {

    "call the specified url, parse the result and return an entity with the correct result" in new TestContext {

      val expectedBody = UpstreamResponse(Map("EUR" -> 1.11),"GBP","2020-01-20").asJson.noSpaces
      val userRequest = ConversionRequest("GBP", "EUR", 102.6)

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .inScenario("caching scenario")
          .withQueryParam("base", equalTo("GBP"))
          .withQueryParam("symbols", equalTo("EUR"))
          .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(expectedBody).withStatus(200)))

      val toRun = run[ConversionResult](userRequest, 200) _
      clientBuilder.resource.use(toRun).unsafeRunSync() should beRight(ConversionResult(1.11,113.886,102.6))
    }

    "cache upstream responses for the test scenario cache TTL of 2 seconds" in new TestContext {

      val expectedCachedBody = UpstreamResponse(Map("USD" -> 1.50), "GBP", "2020-01-20").asJson.noSpaces
      val expectedBodyAfterTTL = UpstreamResponse(Map("USD" -> 2.00), "GBP", "2020-01-20").asJson.noSpaces

      val userRequest = ConversionRequest("GBP", "USD", 100.0)

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .withQueryParam("base", equalTo("GBP"))
          .withQueryParam("symbols", equalTo("USD"))
          .inScenario("caching scenario")
          .whenScenarioStateIs(Scenario.STARTED)
          .willSetStateTo("second")
          .willReturn(
            aResponse().withHeader("Content-Type", "application/json").withBody(expectedCachedBody).withStatus(200))
      )

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .withQueryParam("base", equalTo("GBP"))
          .withQueryParam("symbols", equalTo("USD"))
          .inScenario("caching scenario")
          .whenScenarioStateIs("second")
          .willSetStateTo(Scenario.STARTED)
          .willReturn(
            aResponse().withHeader("Content-Type", "application/json").withBody(expectedBodyAfterTTL).withStatus(200))
      )

      val toRun = clientBuilder.resource.use(run[ConversionResult](userRequest, 200) _)

      toRun.unsafeRunSync() should beRight(ConversionResult(1.50,150,100.0))
      Thread.sleep(250)
      toRun.unsafeRunSync() should beRight(ConversionResult(1.50,150,100.0))
      Thread.sleep(1000)
      toRun.unsafeRunSync() should beRight(ConversionResult(2.00,200,100.0))
    }

    "handle a bad request response from upstream" in new TestContext {

      val expectedBody = Error("Base 'asdf' is not supported.").asJson.noSpaces
      val userRequest = ConversionRequest("asdf", "EUR", 102.6)

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .withQueryParam("base", equalTo("asdf"))
          .withQueryParam("symbols", equalTo("EUR"))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(expectedBody)
            .withStatus(400)))

      val toRun = run[Error](userRequest, 400) _
      clientBuilder.resource.use(toRun).unsafeRunSync() should beRight(Error("Request to the upstream service failed"))
    }

    "handle a 404 response from upstream" in new TestContext {
      val expectedBody = Error("Error: Requested URL /oil/latsest not found").asJson.noSpaces
      val userRequest = ConversionRequest("asdf", "EUR", 102.6)

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .withQueryParam("base", equalTo("asdf"))
          .withQueryParam("symbols", equalTo("EUR"))
          .willReturn(aResponse()
            .withBody(expectedBody)
            .withStatus(404)))

      val toRun = run[Error](userRequest, 500) _
      clientBuilder.resource.use(toRun).unsafeRunSync() should beRight(Error("Request to the upstream service failed"))
    }
  }

  trait TestContext {

    val path = s"/latest"

    val uri = Uri.fromString(s"http://localhost:$appPort/api/convert").value
    val clientBuilder = BlazeClientBuilder[IO](global)

    def requestWithEntity[T : EntityEncoder[IO, *]](entity: T) = Request[IO](method = Method.POST, uri = uri).withEntity(entity)

    def run[T: EntityDecoder[IO, *]](userRequest: ConversionRequest,
                                     expectedStatus: Int)(client: Client[IO]): IO[Either[DecodeFailure, T]] = {
      client.fetch[Either[DecodeFailure, T]](requestWithEntity(userRequest)) {
        case r =>
          r.status.code shouldBe expectedStatus
          r.attemptAs[T].value
      }
    }
  }
}
