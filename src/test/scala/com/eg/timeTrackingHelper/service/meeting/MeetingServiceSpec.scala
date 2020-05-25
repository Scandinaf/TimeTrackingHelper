package com.eg.timeTrackingHelper.service.meeting

import java.time.LocalDate

import cats.effect.IO
import cats.implicits._
import com.eg.timeTrackingHelper.configuration.model.KeywordMapping
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.jira.model.TicketId
import com.eg.timeTrackingHelper.repository.meeting.MeetingRepository
import com.eg.timeTrackingHelper.repository.meeting.model.Meeting
import com.eg.timeTrackingHelper.repository.meeting.outlook.exception.UnknownException
import com.eg.timeTrackingHelper.service.model.{ActivityType, WorklogEntity}
import org.mockito.MockitoSugar
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class MeetingServiceSpec extends AnyFlatSpec
  with MockitoSugar
  with Matchers
  with EitherValues {

  private val meetingRepositoryMock = mock[MeetingRepository]
  private val defaultTicket = "defaultTicket"
  private val ticket1 = "ticket1"
  private val ticket2 = "ticket2"
  private val keywordMapping = KeywordMapping(
    defaultTicket,
    Map(
      (ticket1, Set("test-1", "test-2", "test-3")),
      (ticket2, Set("test-4", "test-5", "test-6"))
    )
  )
  private val meetingService = MeetingService(meetingRepositoryMock, keywordMapping)

  "MeetingService.getTicketIdBySubject" should "return correctly ticketId based on letter subject" in {
    meetingService.getTicketIdBySubject("A small discussion about test-2 and test-5".some) should be(TicketId(ticket1).some)
    meetingService.getTicketIdBySubject("A small discussion about test-3".some) should be(TicketId(ticket1).some)
    meetingService.getTicketIdBySubject("A small discussion about test-5 and test-2".some) should be(TicketId(ticket1).some)
    meetingService.getTicketIdBySubject("A small discussion about test-4".some) should be(TicketId(ticket2).some)
  }

  it should "return the ticket based on a partial match" in {
    meetingService.getTicketIdBySubject("A small discussion about test-25".some) should be(TicketId(ticket1).some)
    meetingService.getTicketIdBySubject("A small discussion about test-43".some) should be(TicketId(ticket2).some)
  }

  it should "return default ticketId" in {
    meetingService.getTicketIdBySubject("A small discussion about vacations".some) should be(TicketId(defaultTicket).some)
    meetingService.getTicketIdBySubject(None) should be(None)
  }

  "MeetingService.getLogEntityByMeeting" should "return LogEntity by Meeting" in {
    val start = LocalDate.of(2020, 3, 31)
      .atStartOfDay
      .plusHours(10)
    val end = start.plusHours(1)
    val subject = "A small discussion about vacations".some
    val meeting = Meeting(
      subject,
      isTakePlace = true,
      start,
      end,
      1 hour
    )
    val logEntity = WorklogEntity(
      start,
      end,
      TicketId(defaultTicket),
      ActivityType.Major,
      subject
    )
    meetingService.getLogEntityByMeeting(meeting) should be(logEntity.some)
  }

  "MeetingService.getMeetingsLogEntity" should "return Map[LocalDate, List[WorklogEntity]] with upcoming meetings" in {
    val start = LocalDate.of(2020, 3, 31)
    val end = start.plusDays(3)
    val datePeriod = DatePeriod(start, end)
    val meetingStart = start.atStartOfDay.plusHours(10)
    val meetings = List(
      Meeting(
        "meeting #1".some,
        isTakePlace = true,
        meetingStart,
        meetingStart.plusHours(1),
        1 hour
      ),
      Meeting(
        "meeting #2".some,
        isTakePlace = true,
        meetingStart.plusDays(1),
        meetingStart.plusDays(1).plusHours(1),
        1 hour
      ),
      Meeting(
        "meeting test-6 #1.1".some,
        isTakePlace = true,
        meetingStart.plusHours(1).plusMinutes(30),
        meetingStart.plusHours(2),
        30 minutes
      ),
      Meeting(
        "meeting #3".some,
        isTakePlace = true,
        meetingStart.plusDays(2),
        meetingStart.plusDays(2).plusHours(1),
        1 hour
      ),
      Meeting(
        "meeting #2.1".some,
        isTakePlace = true,
        meetingStart.plusDays(1).plusHours(2),
        meetingStart.plusDays(1).plusHours(3).plusMinutes(30),
        90 minutes
      )
    )
    when(meetingRepositoryMock.getTakePlaceMeetings(datePeriod)).thenReturn(IO.pure(meetings))
    val result = meetingService.getMeetingsLogEntity(datePeriod).unsafeRunSync
    result.size should be(3)
    result.get(start) should be(Some(List(
      WorklogEntity(
        meetingStart,
        meetingStart.plusHours(1),
        TicketId(defaultTicket),
        ActivityType.Major,
        "meeting #1".some
      ), WorklogEntity(
        meetingStart.plusHours(1).plusMinutes(30),
        meetingStart.plusHours(2),
        TicketId(ticket2),
        ActivityType.Major,
        "meeting test-6 #1.1".some
      ))))
    result.get(start.plusDays(1)) should be(Some(List(
      WorklogEntity(
        meetingStart.plusDays(1),
        meetingStart.plusDays(1).plusHours(1),
        TicketId(defaultTicket),
        ActivityType.Major,
        "meeting #2".some
      ),
      WorklogEntity(
        meetingStart.plusDays(1).plusHours(2),
        meetingStart.plusDays(1).plusHours(3).plusMinutes(30),
        TicketId(defaultTicket),
        ActivityType.Major,
        "meeting #2.1".some
      ))))
    result.get(start.plusDays(2)) should be(Some(List(
      WorklogEntity(
        meetingStart.plusDays(2),
        meetingStart.plusDays(2).plusHours(1),
        TicketId(defaultTicket),
        ActivityType.Major,
        "meeting #3".some
      ))))
  }

  it should "return exception(Negative scenario)" in {
    val start = LocalDate.of(2020, 3, 31)
    val end = start.plusDays(3)
    val datePeriod = DatePeriod(start, end)
    val unknownException = UnknownException(new RuntimeException("Boom"))
    when(meetingRepositoryMock.getTakePlaceMeetings(datePeriod))
      .thenReturn(IO.raiseError[List[Meeting]](unknownException))
    meetingService.getMeetingsLogEntity(datePeriod).attempt.unsafeRunSync should be(unknownException.asLeft)
  }
}
