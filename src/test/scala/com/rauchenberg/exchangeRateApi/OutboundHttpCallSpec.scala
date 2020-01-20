package com.example.exchangeRateApi

import cats.effect._
import cats.effect.IO._
import fs2.Stream

import scala.concurrent.ExecutionContext.global
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import org.http4s.EntityDecoder
import org.scalatest.matchers.should.Matchers

class OutboundHttpCallSpec extends AnyWordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with Matchers {

  lazy val wireMockPort: Int                = 6789
  private lazy val wireMock: WireMockServer = new WireMockServer(wireMockPort)

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.start()
  }
  override def afterEach(): Unit = {
    wireMock.resetAll()
    super.afterEach()
  }
  override def afterAll(): Unit = {
    wireMock.stop()
    super.afterAll()
  }

  "an outbound http call" should {
    "call the specified url, parse the result and return an entity" in new TestContext {

      val path = s"/some/api"
      val expectedBody = """{"name":"cupcat"}"""

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

      streamResult.headOption shouldBe Option(Left(404, Error("Request failed with status 404")))
    }

    "return an error with status code 500 for an invalid uri" in new TestContext {

      val path = s"some/api"
      val expectedBody = """{"name":"cupcat"}"""

      wireMock.stubFor(
        get(urlPathEqualTo(path))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(expectedBody)
            .withStatus(200)))

      val streamResult = runStream[String](path)

      streamResult.headOption shouldBe Option(Left((500, Error("invalid uri: 'http://localhost:6789some/api'"))))
    }

    "return an error with status code 500 if decoding the upstream response fail" in new TestContext {

      val path = s"/some/api"
      val expectedBody = """{"name":"cupcat"}"""

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

    type IODecoder[T] = EntityDecoder[IO, T]

    def runStream[T : IODecoder](path: String) =
      (for {
        client <- clientBuilder.stream
        sr <- Stream.eval(OutboundHttpCall[IO, T](client, s"http://localhost:$wireMockPort$path"))
      } yield sr).compile.toList.unsafeRunSync()
  }

}
