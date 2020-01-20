package com.example.exchangeRateApi

import scala.math.BigDecimal.RoundingMode

object RateConverter {

  def apply(scale: Int)(rate: BigDecimal, amount: BigDecimal): ConversionResult = {
    val res = rate * amount
    ConversionResult(rate, res.setScale(scale, RoundingMode.HALF_EVEN), amount)
  }

}
