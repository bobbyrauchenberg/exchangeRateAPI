package com.example.exchangeRateApi

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import org.scalacheck.{Arbitrary, Gen}

class RateConverterSpec extends UnitSpecBase {

  implicit val boundedIntArbitrary = Arbitrary(Gen.choose(1, 5))

  "rate conversion" should {
    "correctly calculate the exchange rate" in {
      val f: (BigDecimal, BigDecimal) => ConversionResult = RateConverter(3)
      val res = f(1.11, 102.6)
      res shouldBe ConversionResult(1.11, 113.886, 102.6)
    }

    "apply the correct scale to the returned amount" in {
      forAll { (scale: Int, rate: Double, amount: Double) =>
        whenever(scale != 0) {
          val f: (BigDecimal, BigDecimal) => ConversionResult = RateConverter(scale)
          val res = f(rate, amount)
          res.amount.toString().split("\\.").last.length shouldBe scale
        }
      }
    }
  }

}
