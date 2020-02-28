package com.rauchenberg.exchangeRateApi.server

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Sync, Timer}
import cats.implicits._
import com.rauchenberg.exchangeRateApi.cache.AsyncCacheWrapper
import com.rauchenberg.exchangeRateApi.converters.RateConverter
import com.rauchenberg.exchangeRateApi.domain.{Rate, UserInterpreter}
import com.rauchenberg.exchangeRateApi.httpClient.OutboundHttpCall
import com.rauchenberg.exchangeRateApi.httpClient.OutboundHttpCall.OutboundCallResult
import com.rauchenberg.exchangeRateApi.modules.Algebras
import com.rauchenberg.exchangeRateApi.server.ExchangeRateServer.bigDecimalScale
import fs2.Stream
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scalacache.Cache
import scalacache.caffeine.CaffeineCache

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

case class Config(ttl: Option[Duration], serviceUri: (String, String) => String)

final class HttpApi[F[_] : Concurrent] private (algebras: Algebras[F]) {
  //TODO: do in style of routes like in : https://github.com/gvolpe/pfps-shopping-cart/blob/master/modules/core/src/main/scala/shop/modules/HttpApi.scala
  val exchangeRateRoute = ExchangeRateRoute.exchangeRateRoute[F](algebras.exchangeRate, RateConverter(bigDecimalScale))

  private val routes: HttpRoutes[F] = exchangeRateRoute

  val httpApp: HttpApp[F] = routes.orNotFound
}

object HttpApi {

  def make[F[_] : Concurrent](algebras: Algebras[F]) = Sync[F].delay(
    new HttpApi[F](algebras)
  )

}

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
      ui <- Stream.eval(UserInterpreter.create[F])
      userRoute = new UserRoutes(ui).routes
      httpApp = ExchangeRateRoute.exchangeRateRoute[F](cachedService, RateConverter(bigDecimalScale))
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(appPort, "0.0.0.0")
        .withHttpApp(Logger.httpApp(true, true)((httpApp <+> userRoute).orNotFound))
        .serve
    } yield exitCode
  }.drain
}