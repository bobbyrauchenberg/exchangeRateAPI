package com.rauchenberg.exchangeRateApi.domain

import cats.implicits._
import cats.syntax.apply._
import cats.effect.Sync
import cats.effect.concurrent.Ref

case class User(username: String, age: Int)
case class UserUpdateAge(age: Int)

trait UserAlgebra[F[_]] {
  def find(username: String): F[Option[User]]
  def save(user: User): F[Unit]
  def updateAge(username: String, age: Int): F[Unit]
}

sealed trait UserError extends Exception
case class UserAlreadyExists(username: String) extends UserError
case class UserNotFound(username: String) extends UserError
case class InvalidUserAge(age: Int) extends UserError

object UserInterpreter {

  def create[F[_]](implicit F: Sync[F]): F[UserAlgebra[F]] =
    Ref.of[F, Map[String, User]](Map.empty).map { state =>
      new UserAlgebra[F] {
        private def validateAge(age: Int): F[Unit] =
          if(age <= 0) F.raiseError(InvalidUserAge(age)) else F.unit

        override def find(username: String): F[Option[User]] =
          state.get.map(_.get(username))

        override def save(user: User): F[Unit] =
          validateAge(user.age).flatMap(_ => find(user.username).flatMap {
              case Some(_) =>
                F.raiseError(UserAlreadyExists(user.username))
              case None =>
                state.update(_.updated(user.username, user))
            }
          )

        override def updateAge(username: String, age: Int): F[Unit] =
          validateAge(age).flatMap(_ =>
            find(username).flatMap {
              case Some(user) =>
                state.update(_.updated(username, user.copy(age = age)))
              case None =>
                F.raiseError(UserNotFound(username))
            }
          )
      }
    }

}