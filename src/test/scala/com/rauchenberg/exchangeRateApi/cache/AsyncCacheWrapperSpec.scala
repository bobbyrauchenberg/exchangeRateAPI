package com.rauchenberg.exchangeRateApi.cache

import cats.Id
import cats.effect.IO
import com.rauchenberg.exchangeRateApi.common.UnitSpecBase
import scalacache.Cache
import scalacache.caffeine.CaffeineCache

import scala.collection.mutable.Queue
import scala.concurrent.duration._

class AsyncCacheWrapperSpec extends UnitSpecBase {

  "the cache wrapper" should {

    "wrap a function with an Async cache with a TTL" in new TestContext {
      val functionWrappedInCached = functionWrappedInCache(Option(50 millis))

      functionWrappedInCached.unsafeRunSync() shouldBe 1
      functionWrappedInCached.unsafeRunSync() shouldBe 1
      Thread.sleep(100)
      functionWrappedInCached.unsafeRunSync() shouldBe 2
    }
  }

  trait TestContext {

    implicit val caffeineCache: Cache[Int] = CaffeineCache[Int]
    val mutableQueue = Queue(1, 2)

    def f = IO(mutableQueue.dequeue)

    def functionWrappedInCache(ttl: Option[Duration]) = AsyncCacheWrapper[Int, IO, Id]("cacheKey", ttl, f)

  }
}
