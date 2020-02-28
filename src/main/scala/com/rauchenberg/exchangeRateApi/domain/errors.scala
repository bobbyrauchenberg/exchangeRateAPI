//package com.rauchenberg.exchangeRateApi.domain
//
//import cats.ApplicativeError
//import cats.data.{Kleisli, OptionT}
//import org.http4s.{HttpRoutes, Response}
//import org.http4s.dsl.Http4sDsl
//
//import scala.util.control.NoStackTrace
//
//sealed trait UserError extends NoStackTrace
//final case class InvalidRequest(username: String) extends UserError
//final case class UpstreamServerError(username: String) extends UserError
//final case class InvalidUpstreamResponse(age: Int) extends UserError
//
//trait HttpErrorHandler[F[_], E <: Throwable] extends HttpErrorHandler[F, E] with Http4sDsl[F] {
//  def A: ApplicativeError[F, E]
//  def handler: E => F[Response[F]]
//  def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
//    Kleisli { req =>
//      OptionT {
//        A.handleErrorWith(
//          routes.run(req).value
//        )(e =>
//          A.map(handler(e))(Option(_))
//        )
//      }
//    }
//}