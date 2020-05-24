package com.eg.timeTrackingHelper.service

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import com.allantl.jira4s.v2.domain.{LeaveEstimate, WorkLogCreateResponse}
import com.eg.timeTrackingHelper.configuration.ApplicationConfig.{applicationSettings, jiraConfig}
import com.eg.timeTrackingHelper.model._
import com.eg.timeTrackingHelper.repository.jira.JiraRepository
import com.eg.timeTrackingHelper.repository.jira.model.TicketStatus
import com.eg.timeTrackingHelper.service.meeting.MeetingService
import com.eg.timeTrackingHelper.service.model._
import com.eg.timeTrackingHelper.service.ticket.TicketService
import com.typesafe.scalalogging.StrictLogging
import fs2.Stream
import io.jvm.uuid._

import scala.concurrent.duration._

class TimeTrackingService(
                           meetingService: MeetingService
                         )(
                           implicit val contextShift: ContextShift[IO],
                           timer: Timer[IO]
                         )
  extends StrictLogging
    with WorklogPayloadBuilder {

  private val jiraRepository = JiraRepository(jiraConfig)
  private val ticketService = TicketService(
    jiraRepository,
    Assignee(jiraConfig.userName),
    applicationSettings.majorTicketStatuses.map(TicketStatus(_)),
    applicationSettings.minorTicketStatuses.map(TicketStatus(_))
  )

  def logTime(datePeriod: DatePeriod): IO[Unit] =
    for {
      newDatePeriod <- IO(buildNewDatePeriod(datePeriod))
      meetingsMapF <- meetingService.getMeetingsLogEntity(newDatePeriod).start
      ticketMapF <- ticketService.getTicketsLogEntity(newDatePeriod).start
      meetingsMap <- meetingsMapF.join
      ticketMap <- ticketMapF.join
      finalMap <- IO.pure(meetingsMap |+| ticketMap)
      worklogPayloadsStream <- IO(toWorklogPayloads(finalMap, newDatePeriod))
      _ <- updateJira(worklogPayloadsStream)
    } yield ()

  protected def updateJira(stream: Stream[IO, List[WorklogPayloadWithTicket]]): IO[Unit] =
    stream.flatMap(Stream.emits(_).covary[IO])
      .metered(1.second)
      .flatMap(element => Stream.eval(pushWorklog(element)))
      .flatTap(tuple =>
        Stream.eval(logPushResponse(tuple._1, tuple._2))
      ).compile
      .drain

  protected def buildNewDatePeriod(
                                    datePeriod: DatePeriod
                                  ): DatePeriod =
    datePeriod.copy(end = datePeriod.end.plusDays(1))

  protected def pushWorklog(element: WorklogPayloadWithTicket) =
    for {
      uuid <- IO(UUID.randomString)
      _ <- IO(logger.info(s"UUID - $uuid. Try to push the following element, Element - $element."))
      response <- jiraRepository
        .addWorkLog(
          element.ticketId,
          element.payload,
          LeaveEstimate
        ).attempt.map((uuid, _))
    } yield response

  protected def logPushResponse(
                                 uuid: String,
                                 result: Either[Throwable, WorkLogCreateResponse]
                               ): IO[Unit] =
    IO(logger.info(s"UUID - $uuid. Result of the operation - $result"))
}

object TimeTrackingService {
  def apply(
             meetingService: MeetingService
           )(
             implicit contextShift: ContextShift[IO],
             timer: Timer[IO]
           ): TimeTrackingService =
    new TimeTrackingService(meetingService)
}
