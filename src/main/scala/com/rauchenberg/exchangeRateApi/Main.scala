package com.example.exchangeRateApi

import cats.effect.{ExitCode, IO, IOApp}

import scala.concurrent.duration._
import cats.implicits._
import com.rauchenberg.exchangeRateApi.server.ExchangeRateServer

object Main extends IOApp {

  def run(args: List[String]) = {
    val appPort = 8080
    val uri = (from: String, to: String) => s"https://api.exchangeratesapi.io/latest?base=$from&symbols=$to"
    val ttl = Option(1 minute)
    ExchangeRateServer.stream[IO](appPort, uri, ttl).compile.drain.as(ExitCode.Success)
  }

}