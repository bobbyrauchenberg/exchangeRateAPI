package com.rauchenberg.exchangeRateApi.server

import cats.effect.{Async, Sync}
import cats.implicits._
import com.rauchenberg.exchangeRateApi.algebras.ExchangeRate
import com.rauchenberg.exchangeRateApi.domain.{ConversionRequest, ConversionResult, Error}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ExchangeRateRoute {

  def exchangeRateRoute[F[_] : Async : Sync](exchangeRate: ExchangeRate[F],
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
            rate <- exchangeRate.getRate(data.fromCurrency, data.toCurrency)
            resp <- Ok(converterFunction(rate.rate, data.amount))
          } yield resp
        }
    }
  }



}
