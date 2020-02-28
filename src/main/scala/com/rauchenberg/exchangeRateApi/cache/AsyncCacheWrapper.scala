package com.rauchenberg.exchangeRateApi.cache

import cats.implicits._
import cats.effect.Async
import cats.{Monad, Traverse}
import scalacache.{Cache, Mode, get, put}

import scala.concurrent.duration.Duration

trait CacheWrapper[F[_], A] {
  def getFromCache: F[A]
}

final class LiveCacheWrapper[F[_] : Monad : Mode, A : Cache] private (
  cacheKey: String,
  ttl: Option[Duration],
  getValue: => F[A]
) extends CacheWrapper[F, A] {


  override def getFromCache: F[A] = get(cacheKey).flatMap {
    case None =>
      getValue.flatMap { (valueToCache: A) =>
        put[F, A](cacheKey)(valueToCache, ttl).map(_ => valueToCache)
      }
    case Some(v) =>
      Monad[F].pure(v)
  }
}

object AsyncCacheWrapper {

  import scalacache.CatsEffect.modes.async

  def apply[T : Cache, F[_] : Async, M[_] : Monad : Traverse](cacheKey: String,
                                                              ttl: Option[Duration], getValue: => F[M[T]]): F[M[T]] =
      get(cacheKey).flatMap {
        case None =>
          getValue.flatMap(
            valueInMonad =>
              valueInMonad
                .flatTraverse(valueToCache => put[F, T](cacheKey)(valueToCache, ttl).map(_ => valueInMonad))
          )
        case Some(v) =>
          Async[F].pure(Monad[M].pure(v))
      }

}
