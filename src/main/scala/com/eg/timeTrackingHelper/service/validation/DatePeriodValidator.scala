package com.eg.timeTrackingHelper.service.validation

import cats.data.ValidatedNel
import cats.implicits._
import com.eg.timeTrackingHelper.model.DatePeriod

object DatePeriodValidator extends Validator[DatePeriod, String] {
  override def validate(entity: DatePeriod): ValidatedNel[String, DatePeriod] =
    for {
      _ <- List(
        DateValidation.dateMoreThanNow("start")(entity.start),
        DateValidation.dateMoreThanNow("end")(entity.end),
        DateValidation.startBeforeEnd(entity.start, entity.end)
      ).sequence
    } yield entity
}
