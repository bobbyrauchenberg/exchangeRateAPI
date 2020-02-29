package com.rauchenberg.exchangeRateApi.algebras

import cats.MonadError
import cats.effect._
import cats.implicits._
import com.rauchenberg.exchangeRateApi.domain.Rate
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{EntityDecoder, Status, Uri}

case class RateError(msg: String) extends Throwable

trait ExchangeRate[F[_]] {

  def getRate(from: String, to: String): F[Rate]

}

object LiveExchangeRate {
  def make[F[_] : Sync](client: Client[F], uri: String)(implicit ME: MonadError[F, Throwable], ED: EntityDecoder[F, Rate]) =
    Sync[F].delay(new LiveExchangeRate[F](client, uri))
}

final class LiveExchangeRate[F[_]] private(
  client: Client[F],
  uri: String
)(implicit ME: MonadError[F, Throwable], ED: EntityDecoder[F, Rate]) extends ExchangeRate[F] {
  override def getRate(from: String, to: String): F[Rate] = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    Uri.fromString(uri)
      .traverse { u =>
        client.fetch(GET(u)) {
          case Status.Successful(r) =>
            r.attemptAs[Rate].fold(e => RateError(e.getMessage()).raiseError[F, Rate],
              v => ME.pure(v))
          case _ =>
            ME.pure(RateError("can't get exchange rate").raiseError[F, Rate])
        }
      }.flatMap(_.fold(e => RateError(e.getMessage()).raiseError[F, Rate], identity))
  }

}


