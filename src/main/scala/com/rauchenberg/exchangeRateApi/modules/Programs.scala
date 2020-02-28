package com.rauchenberg.exchangeRateApi.modules

import cats.effect.Sync
import com.rauchenberg.exchangeRateApi.algebras.LiveExchangeRate
import org.http4s.client.Client

object Programs {
  def make[F[_]: Sync](client: Client[F], uri: String) = Sync[F].delay {
    new Programs(client, uri)
  }
}

final class Programs[F[_] : Sync] private (val client: Client[F], uri: String) {
  def httpCall = LiveExchangeRate.make[F](client, uri)
}