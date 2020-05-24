package com.eg.timeTrackingHelper.utils

import java.text.SimpleDateFormat
import java.time.{DayOfWeek, LocalDate, LocalDateTime, ZoneId}
import java.util.Date

import cats.syntax.option._
import com.eg.timeTrackingHelper.model.{DatePeriod, DateTimePeriod}

import scala.collection.JavaConverters._
import scala.util.Try

object DateTimeHelper {
  val defaultZoneId = ZoneId.systemDefault()

  implicit class LocalDateToDate(localDate: LocalDate) {
    def toDate: Date =
      Date.from(localDate.atStartOfDay(defaultZoneId).toInstant())
  }

  implicit class StrToLocalDateTime(
                                     date: String
                                   )(
                                     implicit jiraDateFormat: SimpleDateFormat
                                   ) {
    def toLDT: Either[Throwable, LocalDateTime] =
      Try(jiraDateFormat
        .parse(date))
        .toEither
        .map(_.toLocalDateTime)
  }

  implicit class DateToLocalDateTime(date: Date) {
    def toLocalDateTime: LocalDateTime =
      date.toInstant.atZone(defaultZoneId).toLocalDateTime
  }

  implicit class DatePeriodCompanion(datePeriod: DatePeriod) {
    def toListLocalDate: List[LocalDate] =
      datePeriod
        .start
        .datesUntil(datePeriod.end)
        .iterator
        .asScala
        .toList
  }

  def buildPeriod(localDate: LocalDate): DateTimePeriod =
    DateTimePeriod(
      localDate.atStartOfDay(),
      localDate.plusDays(1).atStartOfDay()
    )

  def isNotWeekend(localDate: LocalDate): Boolean =
    !isWeekend(localDate)

  def isWeekend(localDate: LocalDate): Boolean =
    isWeekend(localDate.getDayOfWeek)

  def scaleToTimeFrame(
                        duration: Int,
                        frame: Int
                      ): Int =
    duration.some.filterNot(_ <= 0).map {
      duration =>
        ((BigDecimal(duration) / frame)
          .setScale(0, BigDecimal.RoundingMode.UP) * frame).toInt
    }.getOrElse(frame)

  private def isWeekend(dayOfWeek: DayOfWeek): Boolean =
    dayOfWeek == DayOfWeek.SATURDAY ||
      dayOfWeek == DayOfWeek.SUNDAY
}