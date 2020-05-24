package com.eg.timeTrackingHelper.repository.meeting.mip

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, ContextShift, IO}
import cats.syntax.option._
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.meeting.MeetingRepository
import com.eg.timeTrackingHelper.repository.meeting.mip.codec.JsonCodec._
import com.eg.timeTrackingHelper.repository.meeting.model.Meeting
import com.eg.timeTrackingHelper.routes.exception.AccessTokenEmptyException
import com.eg.timeTrackingHelper.utils.AccessTokenHelper
import com.typesafe.scalalogging.StrictLogging
import org.http4s.Uri.RegName
import org.http4s._
import org.http4s.client.{Client, JavaNetClientBuilder}

private[meeting] class OutlookMeetingRepository(
                                                 tokenRef: Ref[IO, Option[String]],
                                                 blocker: Blocker
                                               )(
                                                 implicit context: ContextShift[IO]
                                               ) extends MeetingRepository with StrictLogging {
  private val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create

  override def getMeetings(datePeriod: DatePeriod): IO[List[Meeting]] =
    for {
      accessToken <- AccessTokenHelper.getAccessToken(tokenRef)
      request <- buildRequest(accessToken, datePeriod)
      result <- httpClient.expect[List[Meeting]](request)
    } yield result

  private def buildRequest(accessToken: String, datePeriod: DatePeriod): IO[Request[IO]] =
    IO(
      Request[IO](
        uri = buildTokenUri(datePeriod),
        headers = Headers(List(Header("Authorization", s"Bearer $accessToken")))
      ))

  private def buildTokenUri(datePeriod: DatePeriod): Uri =
    Uri(
      scheme = Uri.Scheme.https.some,
      authority = Uri.Authority(host = RegName("graph.microsoft.com")).some,
      path = "v1.0/me/calendarview",
      query = Query.fromPairs(
        "startdatetime" -> datePeriod.start.atStartOfDay.toString,
        "enddatetime" -> datePeriod.end.atStartOfDay.toString,
      )
    )
}

object OutlookMeetingRepository {
  def apply(
             tokenRef: Ref[IO, Option[String]],
             blocker: Blocker,
           )(
             implicit context: ContextShift[IO]
           ): OutlookMeetingRepository =
    new OutlookMeetingRepository(tokenRef, blocker)
}
