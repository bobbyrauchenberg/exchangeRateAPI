package com.example.exchangeRateApi

import cats.Functor
import cats.effect.{Async, Sync}
import cats.implicits._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.Method._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{EntityDecoder, Status, Uri}


object OutboundHttpCall {

  type HttpCallError[T] = Either[(Int, Error), T]

  def apply[F[_] : Functor : Sync : Async, T](httpClient: Client[F], uri: String)(implicit decoder: EntityDecoder[F, T]): F[HttpCallError[T]] = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    Uri.fromString(uri).leftMap(_ => (500, Error(s"invalid uri: '$uri'")))
      .flatTraverse { u =>
        httpClient.fetch[HttpCallError[T]](GET(u)) {
          case Status.Successful(r) =>
            r.attemptAs[T]
              .leftMap(_ => (500, Error("Failed to decode upstream response body"))).value
          case r => Functor[F].map(r.as[String])(_ => Left((r.status.code, Error(s"Request failed with status ${r.status.code}"))))
        }
      }
  }
}