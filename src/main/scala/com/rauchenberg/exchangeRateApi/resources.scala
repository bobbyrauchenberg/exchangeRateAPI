package com.rauchenberg.exchangeRateApi

import cats.effect.ConcurrentEffect
import com.rauchenberg.exchangeRateApi.server.Config
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global

final case class AppResources[F[_]](
  client: Client[F]
)

object AppResources {

  def make[F[_] : ConcurrentEffect](config: Config) = {

    val httpClient = BlazeClientBuilder[F](global).resource

    httpClient.map(AppResources[F])

  }
}