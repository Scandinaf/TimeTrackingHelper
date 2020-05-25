package com.eg.timeTrackingHelper.repository.meeting.mip.codec

import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.concurrent.TimeUnit

import cats.effect.IO
import com.eg.timeTrackingHelper.repository.meeting.model.Meeting
import com.eg.timeTrackingHelper.utils.{DateTimeHelper, JsonCodecHelper}
import io.circe.Decoder.Result
import io.circe.{ACursor, Decoder, HCursor, Json}
import org.http4s.circe.jsonOf
import cats.implicits._

import scala.concurrent.duration.Duration

private[codec] trait MeetingDecoder
  extends ResponseStatusDecoder
    with InstantDecoder
    with JsonCodecHelper {

  implicit val meetingDecoder = new Decoder[Meeting] {
    override def apply(c: HCursor): Result[Meeting] = for {
      subject <- getSubject(c)
      isTakePlace <- getIsTakePlace(c)
      start <- getStartLocalDateTime(c)
      end <- getEndLocalDateTime(c)
    } yield Meeting(
      subject,
      isTakePlace,
      start,
      end,
      Duration(
        ChronoUnit.MINUTES.between(start, end),
        TimeUnit.MINUTES
      )
    )
  }

  implicit val meetingsDecoder = new Decoder[List[Meeting]] {
    override def apply(c: HCursor): Result[List[Meeting]] =
      getRequiredField[List[Json]](
        c.downField("value").focus,
        "value"
      ).flatMap(_.traverse(_.as[Meeting]))
  }

  implicit val resMeetingsDecoder = jsonOf[IO, List[Meeting]]

  private def getSubject(c: HCursor): Result[Option[String]] =
    getRequiredField[String](
      c.downField("subject").focus,
      "subject"
    ).map(subject =>
      if (subject.isBlank)
        scala.None else
        subject.some
    )

  private def getIsTakePlace(c: HCursor): Result[Boolean] =
    for {
      isCancelled <- getIsCancelled(c)
      responseStatus <- getResponseStatus(c)
    } yield isTakePlace(isCancelled, responseStatus)

  private def getIsCancelled(c: HCursor): Result[Boolean] =
    getRequiredField(
      c.downField("isCancelled").focus,
      "isCancelled"
    )

  private def getResponseStatus(c: HCursor): Result[ResponseStatus] =
    getRequiredField(
      c.downField("responseStatus").downField("response").focus,
      "responseStatus.response"
    )

  private def isTakePlace(isCancelled: Boolean, responseStatus: ResponseStatus): Boolean =
    responseStatus match {
      case Declined => false
      case _ if isCancelled => false
      case _ => true
    }

  private def getStartLocalDateTime(c: HCursor): Result[LocalDateTime] =
    getLocalDateTime(c.downField("start"), "start")

  private def getEndLocalDateTime(c: HCursor): Result[LocalDateTime] =
    getLocalDateTime(c.downField("end"), "end")

  private def getLocalDateTime(c: ACursor, parent: String): Result[LocalDateTime] =
    for {
      dateTime <- getRequiredField[Instant](c.downField("dateTime").focus, s"$parent.dateTime")
      timeZone <- getRequiredField[ZoneId](c.downField("timeZone").focus, s"$parent.timeZone")
    } yield dateTime.atZone(timeZone).withZoneSameInstant(DateTimeHelper.defaultZoneId).toLocalDateTime
}
