package com.rauchenberg.exchangeRateApi.common

import com.github.tomakehurst.wiremock.WireMockServer
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait WireMockSpec extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>

  lazy val port = {
    val start = 4000
    val end = 9000
    val rnd = new scala.util.Random
    start + rnd.nextInt((end - start) + 1)
  }

  lazy val wireMockPort: Int = 6789
  lazy val wireMock: WireMockServer = new WireMockServer(port)

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.start()
  }

  override def afterEach(): Unit = {
    wireMock.resetAll()
    super.afterEach()
  }

  override def afterAll(): Unit = {
    wireMock.stop()
    super.afterAll()
  }


}
