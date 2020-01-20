package com.example.exchangeRateApi

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

object Main extends IOApp {
  def run(args: List[String]) =
    ExchangeRateCalculatorServer.stream[IO].compile.drain.as(ExitCode.Success)
}