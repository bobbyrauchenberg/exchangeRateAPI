package com.example.exchangeRateApi

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.rauchenberg.exchangeRateApi.AppResources
import com.rauchenberg.exchangeRateApi.modules.Algebras
import com.rauchenberg.exchangeRateApi.server.{Config, ExchangeRateServer}

import scala.concurrent.duration._

object Main extends IOApp {

  def run(args: List[String]) = {
    val appPort = 8080
    val uri = (from: String, to: String) => s"https://api.exchangeratesapi.io/latest?base=$from&symbols=$to"
    val ttl = Option(1 minute)
    val cfg = Config(ttl, uri)

    AppResources.make[IO](cfg).use { res =>
      for {
        algebras <- Algebras.make(res.client, "http://blah.com")
      } yield null
    }


    ExchangeRateServer.stream[IO](appPort, uri, ttl).compile.drain.as(ExitCode.Success)
  }

}