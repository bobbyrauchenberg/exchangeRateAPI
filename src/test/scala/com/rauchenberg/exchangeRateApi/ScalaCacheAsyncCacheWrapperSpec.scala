package com.example.exchangeRateApi

import cats.Id
import cats.effect.IO
import scalacache.Cache
import scalacache.caffeine.CaffeineCache

import scala.collection.mutable.Queue
import scala.concurrent.duration._

class ScalaCacheAsyncCacheWrapperSpec extends UnitSpecBase {

  "the cache wrapper" should {

    "wrap a function with an Async cache with a TTL" in new TestContext {
      val functionWrappedInCached = functionWrappedInCache(Option(1 seconds))

      functionWrappedInCached.unsafeRunSync() shouldBe 1
      functionWrappedInCached.unsafeRunSync() shouldBe 1
      Thread.sleep(1100)
      functionWrappedInCached.unsafeRunSync() shouldBe 2
    }
  }

  trait TestContext {
    implicit val caffeineCache: Cache[Int] = CaffeineCache[Int]

    val mutableQueue = Queue(1, 2)
    def f = IO.pure(mutableQueue.dequeue)

    def functionWrappedInCache(ttl: Option[Duration]) = ScalaCacheAsyncCacheWrapper[Int, IO, Id]("cacheKey", ttl, f)

  }
}
