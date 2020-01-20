package com.example.exchangeRateApi

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import com.example.exchangeRateApi.OutboundHttpCall.HttpCallError
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scalacache.Cache
import scalacache.caffeine.CaffeineCache

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.global

case class Config(ttl: Option[Duration], serviceUri: String)

object ExchangeRateCalculatorServer {

  implicit val caffeineCache: Cache[Rate] = CaffeineCache[Rate]

  def stream[F[_]](implicit T: Timer[F], C: ContextShift[F], CE: ConcurrentEffect[F]): Stream[F, Nothing] = {

    val ttl = Option(1 minute)
    def serviceUri(from: String, to: String) = s"https://api.exchangeratesapi.io/latest?base=$from&symbols=$to"

    for {
      client <- BlazeClientBuilder[F](global).stream
      cachedService = (from: String, to: String) => {
        val cacheKey = from + to
        ScalaCacheAsyncCacheWrapper[Rate, F, HttpCallError](
          cacheKey, ttl, OutboundHttpCall[F, Rate](client, serviceUri(from, to))
        )
      }
      httpApp = ExchangeRateConversionRoute.exchangeRateRoute[F](cachedService, RateConverter(3)).orNotFound

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(Logger.httpApp(true, true)(httpApp))
        .serve
    } yield exitCode
  }.drain
}