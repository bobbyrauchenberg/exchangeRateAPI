package com.rauchenberg.exchangeRateApi.programmes

import com.rauchenberg.exchangeRateApi.algebras.{ExchangeRate, HttpCall}

class ExchangeRateProgram[F[_]](exchangeRate: ExchangeRate[F], httpCall: HttpCall[F]) {

}
