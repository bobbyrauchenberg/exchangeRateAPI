package com.example.exchangeRateApi

import cats.effect.Async
import cats.implicits._
import cats.{Monad, Traverse}
import scalacache.{Cache, get, put}

import scala.concurrent.duration.Duration

object ScalaCacheAsyncCacheWrapper {

  import scalacache.CatsEffect.modes.async

  def apply[T, F[_] : Async, M[_] : Monad : Traverse](cacheKey: String, ttl: Option[Duration], getValue: => F[M[T]])(implicit cache: Cache[T]): F[M[T]] = {
      get(cacheKey).flatMap {
        case None => getValue.flatMap(valueInMonad =>
            valueInMonad
              .flatTraverse(valueToCache => put[F, T](cacheKey)(valueToCache, ttl)
              .map(_ => valueInMonad))
        )
        case Some(v) => Async[F].pure(Monad[M].pure(v))
      }
    }
}

