package com.rauchenberg.exchangeRateApi.server

import cats.effect.IO
import io.circe.syntax._
import cats.syntax.either._
import com.rauchenberg.exchangeRateApi.common.UnitSpecBase
import com.rauchenberg.exchangeRateApi.domain.{ConversionRequest, ConversionResult, Error, Rate}
import org.http4s._
import org.http4s.implicits._
import org.scalatest.prop.TableDrivenPropertyChecks

class ExchangeRateConversionRouteSpec extends UnitSpecBase with TableDrivenPropertyChecks {

  "the conversion route" should {
    "accept a POST request with a body which is parsed and passed to a rate converter" in new TestContext {

      val conversionRequest = (_: String, _: String) => IO.pure(Rate("theToCurrency", 100).asRight)
      val converterFunction = (_: BigDecimal, _: BigDecimal) => ConversionResult(1, 2, 3)

      val res = ExchangeRateRoute.exchangeRateRoute[IO](conversionRequest, converterFunction).orNotFound(req)
      res.unsafeRunSync().status.code shouldBe 200
      res.unsafeRunSync().as[ConversionResult].unsafeRunSync() shouldBe ConversionResult(1,2,3)
    }

    "return a mapped error if the outbound http client errors" in new TestContext {
      val errorCodes = Table(
        ("httpClientErrorCode", "responseStatusCode"),
        (404, 500),
        (400, 400),
        (500, 500),
        (401, 500),
        (403, 500),
        (503, 500)
      )
      forAll(errorCodes) { (httpClientErrorCode, responseCode) =>

        val conversionRequest = (_: String, _: String) => IO.pure((httpClientErrorCode, Error("outbound call failed")).asLeft)
        val converterFunction = (_: BigDecimal, _: BigDecimal) => ConversionResult(1, 2, 3)

        val res = ExchangeRateRoute.exchangeRateRoute[IO](conversionRequest, converterFunction).orNotFound(req)
        res.unsafeRunSync().status.code shouldBe responseCode
        res.unsafeRunSync().as[Error].unsafeRunSync() shouldBe Error("outbound call failed")
      }
    }
  }

  trait TestContext {
    val requestBody = ConversionRequest("anyTestValue", "theToCurrency", 100).asJson.noSpaces
    val req = Request[IO](Method.POST, uri"/api/convert").withEntity(requestBody)
  }

}
