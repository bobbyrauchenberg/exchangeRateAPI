//package com.rauchenberg.exchangeRateApi.modules
//
//import cats.effect.Sync
//import com.rauchenberg.exchangeRateApi.domain.Rate
//
//object MiddleWare {
//  def make[F[_] : Sync](httpCall: HttpCall[F, Rate]) =
//    Sync[F].delay(
//      new MiddleWare[F](httpCall)
//    )
//}
//
//final class MiddleWare[F[_]] private (
//  val httpCall: HttpCall[F, Rate] ) {}
