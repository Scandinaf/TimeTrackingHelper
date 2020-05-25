package com.eg.timeTrackingHelper.utils.converter

import java.util.concurrent.TimeUnit

import cats.implicits._
import com.eg.timeTrackingHelper.repository.meeting.model
import com.eg.timeTrackingHelper.repository.meeting.model.Meeting
import com.eg.timeTrackingHelper.utils.DateTimeHelper.DateToLocalDateTime
import com.typesafe.scalalogging.StrictLogging
import microsoft.exchange.webservices.data.core.enumeration.property.MeetingResponseType
import microsoft.exchange.webservices.data.core.service.item.Appointment

import scala.concurrent.duration.Duration

object Converter extends StrictLogging {

  sealed trait Converter[A, B] {
    def convert(a: A): Either[ConvertException, B]
  }

  implicit val appointmentToMeeting = new Converter[Appointment, Meeting] {
    override def convert(a: Appointment): Either[ConvertException, Meeting] =
      Either.catchNonFatal {
        model.Meeting(
          convertSubject(a.getSubject),
          isTakePlace(a),
          a.getStart.toLocalDateTime,
          a.getEnd.toLocalDateTime,
          Duration(a.getDuration.getTotalMinutes, TimeUnit.MINUTES)
        )
      }.leftMap(throwable => {
        logger.error(
          s"""Unexpected behavior, an error occurred during the conversion.
             | Appointment : {subject : ${a.getSubject},
             |  startDate : ${a.getStart},
             |   endDate : ${a.getEnd}}.""".stripMargin,
          throwable
        )
        UnexpectableBehaviourException(throwable)
      })

    private def convertSubject(subject: String): Option[String] =
      if (subject.isBlank) None else subject.some

    private def isTakePlace(a: Appointment): Boolean =
      !(a.getIsCancelled || a.getMyResponseType == MeetingResponseType.Decline)
  }

  def convertTo[A, B](a: A)(implicit converter: Converter[A, B]): Either[ConvertException, B] =
    converter.convert(a)
}
