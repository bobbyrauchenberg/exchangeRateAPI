package com.rauchenberg.exchangeRateApi.server

import cats.implicits._
import cats.effect.{Async, Sync}
import com.rauchenberg.exchangeRateApi.domain.{ConversionRequest, ConversionResult, Error, Rate}
import com.rauchenberg.exchangeRateApi.httpClient.OutboundHttpCall.OutboundCallResult
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ExchangeRateRoute {

  def exchangeRateRoute[F[_] : Async : Sync](getExchangeRate: (String, String) => F[OutboundCallResult[Rate]],
                                             converterFunction: (BigDecimal, BigDecimal) => ConversionResult): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    def mapErrors(statusAndError: (Int, Error)) = {
      val (code, error) = statusAndError
      code match {
        case 400 => BadRequest(error)
        case _ => InternalServerError(error)
      }
    }

    HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "convert" =>
        req.decode[ConversionRequest] { data =>
          for {
            exchangeRateServiceResponse <- getExchangeRate(data.fromCurrency, data.toCurrency)
            rate <- Sync[F].pure(exchangeRateServiceResponse.map(_.rate))
            resp <- rate.fold(mapErrors, rate => Ok(converterFunction(rate, data.amount)))
          } yield resp
        }
    }
  }



}
