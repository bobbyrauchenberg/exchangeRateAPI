package com.rauchenberg.exchangeRateApi.server

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.apply._
import cats.syntax.applicativeError._
import com.rauchenberg.exchangeRateApi.domain.{InvalidUserAge, User, UserAlgebra, UserAlreadyExists, UserUpdateAge}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class UserRoutes[F[_]: Sync](userAlgebra: UserAlgebra[F]) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "users" /username =>
      userAlgebra.find(username).flatMap {
        case Some(user) => Ok(user.asJson)
        case None => NotFound(username.asJson)
      }

    case req @ POST -> Root / "users" =>
      req.as[User].flatMap { user =>
        userAlgebra.save(user) *> Created(user.username.asJson)
      }.handleErrorWith {
        case UserAlreadyExists(username) => Conflict(username.asJson)
      }

    case req @ PUT -> Root / "users" / username =>
      req.as[UserUpdateAge].flatMap { userUpdate =>
        userAlgebra.updateAge(username, userUpdate.age) *> Ok(username)
      }.handleErrorWith {
        case InvalidUserAge(age) => BadRequest(s"Invalid age $age".asJson)
      }
  }

}
