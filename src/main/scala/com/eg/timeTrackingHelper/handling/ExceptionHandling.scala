package com.eg.timeTrackingHelper.handling

import cats.Show
import cats.effect.IO
import com.eg.timeTrackingHelper.service.validation.exception.ValidationException

trait ExceptionHandling[A] {
  protected val lineSeparator = sys.props("line.separator")

  def handle(exception: Throwable): IO[A]

  implicit val validationExceptionShow = new Show[ValidationException] {
    override def show(exception: ValidationException): String =
      s"""During the validation process, the following errors were detected:
         |${exception.listError.toList.mkString(lineSeparator)}
         |""".stripMargin
  }
}
