package com.rauchenberg.exchangeRateApi.programmes

import com.rauchenberg.exchangeRateApi.algebras.ExchangeRate

class ExchangeRateProgram[F[_]](exchangeRate: ExchangeRate[F]) {}
