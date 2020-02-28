//package com.rauchenberg.exchangeRateApi.algebras
//
//import cats.effect.Sync
//import cats.implicits._
//import org.http4s.Method.GET
//import org.http4s.client.Client
//import org.http4s.client.dsl.Http4sClientDsl
//import org.http4s.{EntityDecoder, Status, Uri}
//
//trait HttpCall[F[_], A] {
//  def doCall: F[A]
//}
//
//final class LiveHttpCall[F[_] : Sync, A : EntityDecoder[F, *]] private (httpClient: Client[F], uri: String) extends HttpCall[F, A] {
//
//  val dsl = new Http4sClientDsl[F] {}
//  import dsl._
//
//  override def doCall: F[A] = Uri.fromString(uri).map { uri =>
//    httpClient.fetch(GET(uri)) {
//      case Status.Successful(r) =>
//        r.attemptAs[A].fold(e => Sync[F].raiseError[A](HttpResponseDecodeError(e.message)), s => Sync[F].pure(s)).flatten
//      case _ => Sync[F].raiseError[A](HttpCallError)
//    }
//  }.fold(e => Sync[F].raiseError[A](UriParseFailure(e.message)), identity)
//
//}
