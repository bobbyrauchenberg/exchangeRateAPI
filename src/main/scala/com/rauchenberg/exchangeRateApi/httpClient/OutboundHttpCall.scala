package com.rauchenberg.exchangeRateApi.httpClient

import cats.Functor
import cats.effect.{Async, Sync}
import cats.implicits._
import com.rauchenberg.exchangeRateApi.domain.Error
import org.http4s.Method._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{EntityDecoder, Status, Uri}


object OutboundHttpCall {

  type OutboundCallResult[T] = Either[(Int, Error), T]

  def apply[F[_] : Functor : Sync : Async, T : EntityDecoder[F, *]](httpClient: Client[F],
                                                                    uri: String): F[OutboundCallResult[T]] = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    def serverError(msg: String) = (500, Error(msg))

    Uri.fromString(uri).leftMap(_ => serverError(s"invalid uri: '$uri'"))
      .flatTraverse { u =>
        httpClient.fetch[OutboundCallResult[T]](GET(u)) {
          case Status.Successful(r) =>
            r.attemptAs[T].leftMap(_ => serverError("Failed to decode upstream response body")).value
          case r => Functor[F].map(r.as[String])(_ => Left((r.status.code, Error(s"Request to the upstream service failed"))))
        }
      }
  }
}