package com.eg.timeTrackingHelper.handling

import cats.effect.IO
import com.eg.timeTrackingHelper.service.validation.exception.ValidationException
import org.http4s.{InvalidMessageBodyFailure, MalformedMessageBodyFailure, Response}
import cats.implicits._
import com.eg.timeTrackingHelper.routes.exception.AccessTokenEmptyException
import io.circe.DecodingFailure
import org.http4s.dsl.io._
import org.http4s.dsl.io.BadRequest

trait RestExceptionHandling extends ExceptionHandling[Response[IO]] {
  override def handle(exception: Throwable): IO[Response[IO]] =
    exception match {
      case e: ValidationException => BadRequest(e.show)
      case _: AccessTokenEmptyException =>
        BadRequest("""There is no point in continuing because the token hasn't been installed.
            |Please visit the next page 'GET /timeTrackingHelper/token'.""".stripMargin)
      case InvalidMessageBodyFailure(_, Some(DecodingFailure(message, _))) => BadRequest(message)
      case InvalidMessageBodyFailure(details, _)                           => BadRequest(details)
      case MalformedMessageBodyFailure(details, _)                         => BadRequest(details)
      case _                                                               => InternalServerError("You mustn't see this, please report about problem.")
    }
}
