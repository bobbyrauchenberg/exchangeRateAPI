package com.rauchenberg.exchangeRateApi.algebras

import cats.{Applicative, ApplicativeError, Functor}
import cats.effect._
import cats.implicits._
import com.rauchenberg.exchangeRateApi.domain.{Error, Rate}
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Status, Uri}

case class RateError(msg: String) extends Throwable

trait ExchangeRate[F[_]] {

  def getRate(from: String, to: String): F[Rate]

}

object LiveExchangeRate {
  def make[F[_]](client: Client[F], uri: String)(implicit F: ApplicativeError[F, Rate]) =
    new LiveExchangeRate[F](client, uri)
}

final class LiveExchangeRate[F[_]] private(
  client: Client[F],
  uri: String
)(implicit F: ApplicativeError[F, Rate]) extends ExchangeRate[F] {
  override def getRate(from: String, to: String): F[Rate] = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    Uri.fromString(uri)
      .traverse { u =>
        client.fetch(GET(u)) {
          case Status.Successful(r) =>
            r.attemptAs[Rate].fold(e => RateError(e.getMessage()).raiseError[F, Rate],
              v => F.pure(v)).flatten
          case r =>
            Functor[F].map(r.as[String])(s => RateError(s).raiseError[F, Rate]).flatten
        }
      }.flatMap(_.fold(e => RateError(e.getMessage()).raiseError[F, Rate], v => F.pure(v)))
  }

}


