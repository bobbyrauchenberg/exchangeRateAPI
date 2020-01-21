package com.rauchenberg.exchangeRateApi.common

import cats.scalatest.{EitherMatchers, EitherValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait UnitSpecBase extends AnyWordSpecLike with Matchers with ScalaCheckPropertyChecks with EitherValues with EitherMatchers
