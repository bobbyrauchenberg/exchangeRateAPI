package com.example.exchangeRateApi

import cats.syntax.either._
import io.circe.DecodingFailure

class RateDecodingSpec extends UnitSpecBase {

  "rate decoding" should {

    "handle a valid body" in {
      val json = """{"rates":{"GBP":123},"base":"EUR","date":"2020-01-20"}"""
      val res = io.circe.parser.decode(json)(Rate.decoder)

      res should beRight(Rate("GBP", 123))
    }

    "fail gracefully" in {
      val json = """{"rates":{},"base":"EUR","date":"2020-01-20"}"""
      val res = io.circe.parser.decode(json)(Rate.decoder)

      res shouldBe DecodingFailure("couldn't parse result", List()).asLeft
    }
  }

}
