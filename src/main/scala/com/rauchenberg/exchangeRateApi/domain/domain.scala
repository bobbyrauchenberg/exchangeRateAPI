package com.rauchenberg.exchangeRateApi.domain

import cats.Applicative
import cats.effect.Sync
import cats.syntax.either._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

final case class Error(errorMsg: String)
object Error {
  implicit val decoder: Decoder[Error] = deriveDecoder[Error]
  implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, Error] = jsonOf
  implicit val encoder: Encoder[Error] = deriveEncoder[Error]
  implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, Error] =
    jsonEncoderOf
}

final case class Rate(countryCode: String, rate: BigDecimal)
object Rate {
  /*
    this is needed to handle the fact that the name of the key being decoded is dynamic
    and won't ever map directly to any particular case class field name
   */
  implicit val decoder: Decoder[Rate] =
    Decoder[Map[String, BigDecimal]].prepare(_.downField("rates")).emap { _.map {
        case (k, v) => Rate(k, v)
      }.headOption.fold("couldn't parse result".asLeft[Rate])(_.asRight[String])
    }
  implicit def entityDecoder[F[_]]: EntityDecoder[F, Rate] = jsonOf

  implicit val encoder: Encoder[Rate] = deriveEncoder[Rate]
  implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, Rate] = jsonEncoderOf
}

case class ConversionRequest(fromCurrency: String, toCurrency: String, amount: BigDecimal)
object ConversionRequest {
  implicit val decoder: Decoder[ConversionRequest] = deriveDecoder[ConversionRequest]
  implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, ConversionRequest] =
    jsonOf
  implicit val encoder: Encoder[ConversionRequest] = deriveEncoder[ConversionRequest]
  implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, ConversionRequest] = jsonEncoderOf
}

case class ConversionResult(exchange: BigDecimal, amount: BigDecimal, original: BigDecimal)
object ConversionResult {
  implicit val encoder: Encoder[ConversionResult] = deriveEncoder[ConversionResult]
  implicit def entityEncoder[F[_]: Sync]: EntityEncoder[F, ConversionResult] = jsonEncoderOf

  implicit val decoder: Decoder[ConversionResult] = deriveDecoder[ConversionResult]
  implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, ConversionResult] =
    jsonOf
}
