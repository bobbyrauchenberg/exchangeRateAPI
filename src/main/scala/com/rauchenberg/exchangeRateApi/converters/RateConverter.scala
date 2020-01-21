package com.rauchenberg.exchangeRateApi.converters

import com.rauchenberg.exchangeRateApi.domain.ConversionResult

import scala.math.BigDecimal.RoundingMode

object RateConverter {

  def apply(scale: Int)(rate: BigDecimal, amount: BigDecimal): ConversionResult = {
    val res = rate * amount
    ConversionResult(rate, res.setScale(scale, RoundingMode.HALF_EVEN), amount)
  }

}
