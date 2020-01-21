package com.rauchenberg.exchangeRateApi.httpClient

import cats.effect.IO._
import cats.effect._
import com.github.tomakehurst.wiremock.client.WireMock._
import com.rauchenberg.exchangeRateApi.common.WireMockSpec
import com.rauchenberg.exchangeRateApi.domain.{Error, Rate}
import fs2.Stream
import org.http4s.EntityDecoder
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.global
import io.circe.generic.auto._
import io.circe.syntax._

class OutboundHttpCallSpec extends AnyWordSpecLike with Matchers with WireMockSpec {

  "an outbound http call" should {
    "call the specified url, parse the result and return an entity" in new TestContext {

      val path = s"/some/api"

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(expectedBody)
            .withStatus(200)))


      val streamResult = runStream[String](path)
      streamResult should contain only Right(expectedBody)
    }

    "return an error with the upstream status if the outbound call fails" in new TestContext {

      val path = s"/bad/path"

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .willReturn(aResponse()
            .withStatus(404)))

      val streamResult = runStream[String](path)

      streamResult.headOption shouldBe Option(Left(404, Error("Request to the upstream service failed")))
    }

    "return an error with status code 500 for an invalid uri" in new TestContext {

      val path = s"some/api"

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(expectedBody)
            .withStatus(200)))

      val streamResult = runStream[String](path)

      streamResult.headOption shouldBe Option(Left((500, Error(s"invalid uri: 'http://localhost:${port}some/api'"))))
    }

    "return an error with status code 500 if decoding the upstream response fail" in new TestContext {

      val path = s"/some/api"

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(expectedBody)
            .withStatus(200)))

      val streamResult = runStream[Rate](path)

      streamResult.headOption shouldBe Option(Left(500, Error("Failed to decode upstream response body")))
    }
  }

  trait TestContext {
    implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
    val clientBuilder = BlazeClientBuilder[IO](global)

    case class ExpectedBody(name: String)

    val expectedBody = ExpectedBody("ignore").asJson.noSpaces

    type IODecoder[T] = EntityDecoder[IO, T]

    def runStream[T : IODecoder](path: String) =
      (for {
        client <- clientBuilder.stream
        sr <- Stream.eval(OutboundHttpCall[IO, T](client, s"http://localhost:$port$path"))
      } yield sr).compile.toList.unsafeRunSync()
  }

}
