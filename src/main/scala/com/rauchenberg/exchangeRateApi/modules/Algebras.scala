package com.rauchenberg.exchangeRateApi.modules

import cats.effect._
import cats.implicits._
import com.rauchenberg.exchangeRateApi.algebras.{ExchangeRate, LiveExchangeRate}
import org.http4s.client.Client

object Algebras {

  def make[F[_] : Concurrent](client: Client[F], uri: String): F[Algebras[F]] =
    for {
      er <- LiveExchangeRate.make[F](client, uri)
    } yield new Algebras[F](er)

}

final class Algebras[F[_]] private (val exchangeRate: ExchangeRate[F]) {}
