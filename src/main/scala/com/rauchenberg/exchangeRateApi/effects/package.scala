package com.rauchenberg.exchangeRateApi

import cats.MonadError

package object effects {

  type MonadThrow[F[_]] = MonadError[F, Throwable]

}
