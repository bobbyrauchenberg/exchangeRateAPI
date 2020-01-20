package com.example.exchangeRateApi

import org.http4s.circe.CirceEntityEncoder._
import cats.effect.{Async, Sync}
import cats.implicits._
import com.example.exchangeRateApi.OutboundHttpCall.HttpCallError
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ExchangeRateConversionRoute {

  def exchangeRateRoute[F[_] : Async : Sync](getExchangeRate: (String, String) => F[HttpCallError[Rate]],
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