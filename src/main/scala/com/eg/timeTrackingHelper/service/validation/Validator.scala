package com.eg.timeTrackingHelper.service.validation

import java.time.LocalDate

import cats.data.{Validated, ValidatedNel}

trait Validator[E, R] {

  private val now = LocalDate.now()

  def validate(entity: E): ValidatedNel[R, E]

  protected object DateValidation {
    def startBeforeEnd(start: LocalDate, end: LocalDate): ValidatedNel[String, LocalDate] =
      Validated.condNel(
        start.isBefore(end) || start.isEqual(end),
        start,
        s"The start cannot be less than the end!!!"
      )

    def dateMoreThanNow(fieldName: String)(date: LocalDate): ValidatedNel[String, LocalDate] = {
      Validated.condNel(
        date.isBefore(now) || date.isEqual(now),
        date,
        s"The $fieldName cannot be more than the current date!!!"
      )
    }
  }

}
