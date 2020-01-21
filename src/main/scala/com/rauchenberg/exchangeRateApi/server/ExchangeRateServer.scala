package com.rauchenberg.exchangeRateApi.server

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import com.rauchenberg.exchangeRateApi.cache.AsyncCacheWrapper
import com.rauchenberg.exchangeRateApi.converters.RateConverter
import com.rauchenberg.exchangeRateApi.domain.Rate
import com.rauchenberg.exchangeRateApi.httpClient.OutboundHttpCall
import com.rauchenberg.exchangeRateApi.httpClient.OutboundHttpCall.OutboundCallResult
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scalacache.Cache
import scalacache.caffeine.CaffeineCache

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

case class Config(ttl: Option[Duration], serviceUri: String)

object ExchangeRateServer {

  implicit val caffeineCache: Cache[Rate] = CaffeineCache[Rate]
  val bigDecimalScale = 3

  def stream[F[_] : Timer : ContextShift : ConcurrentEffect](appPort: Int,
                                                             uri: (String, String) => String,
                                                             ttl: Option[Duration]): Stream[F, Nothing] = {

    def serviceUri(from: String, to: String) = uri(from, to)

    for {
      client <- BlazeClientBuilder[F](global).stream
      cachedService = (baseCurrency: String, toCurrency: String) => {
        val cacheKey = baseCurrency + toCurrency
        AsyncCacheWrapper[Rate, F, OutboundCallResult](
          cacheKey, ttl, OutboundHttpCall[F, Rate](client, serviceUri(baseCurrency, toCurrency))
        )
      }
      httpApp = ExchangeRateRoute.exchangeRateRoute[F](cachedService, RateConverter(bigDecimalScale)).orNotFound

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(appPort, "0.0.0.0")
        .withHttpApp(Logger.httpApp(true, true)(httpApp))
        .serve
    } yield exitCode
  }.drain
}