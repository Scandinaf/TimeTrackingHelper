package com.eg.timeTrackingHelper.handling

import java.time.format.DateTimeParseException
import cats.implicits._
import cats.effect.IO
import com.eg.timeTrackingHelper.repository.meeting.outlook.exception.{ServerNotFoundException, UnauthorizedException}
import com.eg.timeTrackingHelper.service.validation.exception.ValidationException

trait ConsoleExceptionHandling extends ExceptionHandling[Unit] {

  override def handle(exception: Throwable): IO[Unit] =
    exception match {
      case _: UnauthorizedException => IO(println("Failed to login, please check your login and password."))
      case _: ServerNotFoundException => IO(println("Couldn't find the mail server, please check the configuration."))
      case _: DateTimeParseException => IO(println("We expect the date to be entered in the following format - YYYY-MM-DD."))
      case e: ValidationException => IO(println(e.show))
      case _ => IO(println("You mustn't see this, please report about problem."))
    }
}
