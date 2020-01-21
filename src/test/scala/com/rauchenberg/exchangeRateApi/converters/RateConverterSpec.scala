package com.rauchenberg.exchangeRateApi.converters

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.rauchenberg.exchangeRateApi.common.UnitSpecBase
import com.rauchenberg.exchangeRateApi.domain.ConversionResult
import org.scalacheck.{Arbitrary, Gen}

import scala.math.BigDecimal.RoundingMode

class RateConverterSpec extends UnitSpecBase {

  implicit val boundedIntArbitrary = Arbitrary(Gen.choose(1, 5))

  "rate conversion" should {
    "correctly calculate the exchange rate" in new TestContext {
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

    "handles any and all generated bigdecimal values" in new TestContext {
      forAll { (exchangeRate: BigDecimal, originalAmount: BigDecimal) =>
        val res = f(exchangeRate, originalAmount)
        val convertedRate = (exchangeRate * originalAmount).setScale(bigDecimalScale, RoundingMode.HALF_EVEN)
        val expected = ConversionResult(exchangeRate, convertedRate, originalAmount)
        res shouldBe expected
      }
    }

  }

  trait TestContext {
    val bigDecimalScale = 3
    val f: (BigDecimal, BigDecimal) => ConversionResult = RateConverter(bigDecimalScale)
  }

}
