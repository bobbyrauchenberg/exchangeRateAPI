package com.rauchenberg.exchangeRateApi.converters

import cats.syntax.either._
import com.rauchenberg.exchangeRateApi.common.{UnitSpecBase, UpstreamResponse}
import com.rauchenberg.exchangeRateApi.domain.Rate
import io.circe.DecodingFailure
import io.circe.syntax._
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._


class RateDecodingSpec extends UnitSpecBase {

  "rate decoding" should {

    "handle a valid body" in {
      forAll { (countryCode1: String, countryCode2: String, date: String, rate: Int) =>

        val json = UpstreamResponse(Map(countryCode1 -> rate), countryCode2, date).asJson.noSpaces
        val res = io.circe.parser.decode(json)(Rate.decoder)

        res should beRight(Rate(countryCode1, rate))
      }
    }

    "fail gracefully" in {
      forAll { (countryCode: String, date: String) =>
        val json = UpstreamResponse(Map.empty, countryCode, date).asJson.noSpaces
        val res = io.circe.parser.decode(json)(Rate.decoder)

        res shouldBe DecodingFailure("couldn't parse result", List()).asLeft
      }
    }
  }

}
