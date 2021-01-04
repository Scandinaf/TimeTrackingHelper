package com.eg.timeTrackingHelper.service.validation

import java.time.LocalDate

import cats.data.NonEmptyList
import com.eg.timeTrackingHelper.model.DatePeriod
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DatePeriodValidatorSpec extends AnyFlatSpec with Matchers with EitherValues {

  private val now = LocalDate.of(2020, 3, 31)

  "DatePeriodValidator" should "correctly validate the same dates" in {
    val period = DatePeriod(now, now)
    DatePeriodValidator.validate(period).toEither.value should be(period)
  }

  it should "correctly validate the different dates" in {
    val period = DatePeriod(now.minusDays(5), now.minusDays(3))
    DatePeriodValidator.validate(period).toEither.value should be(period)
  }

  it should "correctly validate dates larger than the current" in {
    val now = LocalDate.now()
    val period = DatePeriod(now.plusDays(1), now.plusDays(1))
    DatePeriodValidator.validate(period).toEither.left.value should be(
      NonEmptyList.of(
        "The start cannot be more than the current date!!!",
        "The end cannot be more than the current date!!!"
      )
    )
  }

  it should "correctly validate if end date less than start date" in {
    val period = DatePeriod(now, now.minusDays(2))
    DatePeriodValidator.validate(period).toEither.left.value should be(
      NonEmptyList.of("The start cannot be less than the end!!!")
    )
  }
}
