package com.rauchenberg.exchangeRateApi.common

import io.circe.Encoder
import io.circe.generic.semiauto._

case class UpstreamResponse(rates: Map[String, Double], base: String, date: String)

object UpstreamResponse {
  implicit val encoder: Encoder[UpstreamResponse] = deriveEncoder[UpstreamResponse]
}

