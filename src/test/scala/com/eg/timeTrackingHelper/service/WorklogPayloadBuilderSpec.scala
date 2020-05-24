package com.eg.timeTrackingHelper.service

import java.time.LocalDate

import cats.effect.{ContextShift, IO}
import cats.syntax.option._
import com.allantl.jira4s.v2.domain.WorkLogPayLoad
import com.eg.timeTrackingHelper.configuration.ApplicationConfig.applicationSettings.workHoursLimit
import com.eg.timeTrackingHelper.repository.jira.model.TicketId
import com.eg.timeTrackingHelper.service.model.{ActivityType, HierarchicalWorklog, WorklogEntity, WorklogPayloadWithTicket}
import com.eg.timeTrackingHelper.utils.DateTimeHelper
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class WorklogPayloadBuilderSpec extends AnyFlatSpec
  with Matchers
  with EitherValues
  with WorklogPayloadBuilder {

  override implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  val localDate = LocalDate.of(2020, 4, 18)
  val start = localDate.atStartOfDay().plusHours(10)
  val started = localDate.atStartOfDay()
    .plusHours(10)
    .atZone(DateTimeHelper.defaultZoneId)
    .format(dateTimeFormatter)

  "WorklogPayloadBuilder" should "correctly work with one minor ticket" in {
    val ticketId = TicketId("test#1")
    val entity1 = WorklogEntity(
      start = localDate.minusDays(1).atStartOfDay(),
      end = localDate.plusDays(1).atStartOfDay(),
      ticketId = ticketId,
      ActivityType.Minor,
    )

    val hierarchicalWorklog = HierarchicalWorklog(
      major = List.empty,
      minor = List(
        entity1
      )
    )
    val expected = List(
      WorklogPayloadWithTicket(
        ticketId,
        WorkLogPayLoad(None, started, workHoursLimit)
      )
    )
    buildWorkLogPayLoads(localDate, hierarchicalWorklog) should be(expected)
  }

  it should "correctly work with two minor tickets" in {
    val entity1 = WorklogEntity(
      start = localDate.minusDays(1).atStartOfDay(),
      end = localDate.plusDays(1).atStartOfDay(),
      ticketId = TicketId("test#1"),
      ActivityType.Minor,
    )

    val entity2 = WorklogEntity(
      start = localDate.minusDays(1).atStartOfDay(),
      end = localDate.plusDays(1).atStartOfDay(),
      ticketId = TicketId("test#2"),
      ActivityType.Minor,
    )

    val hierarchicalWorklog = HierarchicalWorklog(
      major = List.empty,
      minor = List(
        entity1,
        entity2
      )
    )

    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#2"), WorkLogPayLoad(None, started, 13500)),
      WorklogPayloadWithTicket(TicketId("test#1"), WorkLogPayLoad(None, started, 15300)),
    )
    buildWorkLogPayLoads(localDate, hierarchicalWorklog) should be(expected)
  }

  it should "correctly work with two major tickets that were completed this day" in {
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(2),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )

    val entity2 = WorklogEntity(
      start = entity1.end,
      end = entity1.end.plusHours(6),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )

    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#2"), WorkLogPayLoad(None, started, 6.hours.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#1"), WorkLogPayLoad(None, started, 2.hours.toSeconds.toInt)),
    )
    buildWorkLogPayLoads(
      localDate,
      toHierarchicalWorklogs(List(entity1, entity2))
    ) should be(expected)
  }

  it should "correctly work with two major tickets that were completed this day(with overflow)" in {
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(4),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )

    val entity2 = WorklogEntity(
      start = entity1.end,
      end = entity1.end.plusHours(6),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )

    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#2"), WorkLogPayLoad(None, started, 4.hours.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#1"), WorkLogPayLoad(None, started, 4.hours.toSeconds.toInt)),
    )
    buildWorkLogPayLoads(
      localDate,
      toHierarchicalWorklogs(List(entity1, entity2))
    ) should be(expected)
  }

  it should "correctly work with two major tickets that were completed this day(with overflow) #2" in {
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(4),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )

    val entity2 = WorklogEntity(
      start = entity1.end,
      end = entity1.end.plusHours(6),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )

    val entity3 = WorklogEntity(
      start = entity2.end,
      end = entity2.end.plusHours(1),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )

    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad(None, started, 1.hours.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#2"), WorkLogPayLoad(None, started, 3.hours.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#1"), WorkLogPayLoad(None, started, 4.hours.toSeconds.toInt)),
    )
    buildWorkLogPayLoads(
      localDate,
      toHierarchicalWorklogs(List(entity1, entity2, entity3))
    ) should be(expected)
  }

  it should "correctly work with one major ticket" in {
    val ticketId = TicketId("test#1")
    val entity1 = WorklogEntity(
      start = localDate.minusDays(1).atStartOfDay(),
      end = localDate.plusDays(1).atStartOfDay(),
      ticketId = ticketId,
      ActivityType.Major,
    )

    val hierarchicalWorklog = HierarchicalWorklog(
      major = List(entity1),
      minor = List.empty
    )
    val expected = List(
      WorklogPayloadWithTicket(
        ticketId,
        WorkLogPayLoad(None, started, workHoursLimit)
      )
    )
    buildWorkLogPayLoads(localDate, hierarchicalWorklog) should be(expected)
  }

  it should "correctly work with two major ticket" in {
    val entity1 = WorklogEntity(
      start = localDate.minusDays(1).atStartOfDay(),
      end = localDate.atStartOfDay().plusHours(10),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )

    val entity2 = WorklogEntity(
      start = entity1.end.plusHours(6),
      end = entity1.end.plusDays(1),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )

    val hierarchicalWorklog = HierarchicalWorklog(
      major = List(entity1, entity2),
      minor = List.empty
    )
    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#2"), WorkLogPayLoad(None, started, 4.hour.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#1"), WorkLogPayLoad(None, started, 4.hour.toSeconds.toInt))
    )
    buildWorkLogPayLoads(localDate, hierarchicalWorklog) should be(expected)
  }

  it should "сorrectly process scenario #1" in {
    val entity1 = WorklogEntity(
      start = localDate.minusDays(1).atStartOfDay(),
      end = localDate.atStartOfDay().plusHours(10),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )

    val entity2 = WorklogEntity(
      start = entity1.end.plusHours(1),
      end = entity1.end.plusHours(8).plusMinutes(45),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )

    val entity3 = WorklogEntity(
      start = entity2.end.plusHours(1),
      end = entity2.end.plusDays(1),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )

    val hierarchicalWorklog = HierarchicalWorklog(
      major = List(entity1, entity2, entity3),
      minor = List.empty
    )
    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad(None, started, 15.minutes.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#2"), WorkLogPayLoad(None, started, 465.minutes.toSeconds.toInt)),
    )
    buildWorkLogPayLoads(localDate, hierarchicalWorklog) should be(expected)
  }

  it should "сorrectly merge tickets #1" in {
    val list = List(
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad("#1".some, started, 15.minutes.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad("#2".some, started, 15.minutes.toSeconds.toInt)),
    )

    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad("#1,#2".some, started, 30.minutes.toSeconds.toInt)),
    )

    merge(list) should be(expected)
  }

  it should "сorrectly merge tickets #2" in {
    val list = List(
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad(None, started, 15.minutes.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad(None, started, 15.minutes.toSeconds.toInt)),
    )

    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad(None, started, 30.minutes.toSeconds.toInt)),
    )

    merge(list) should be(expected)
  }

  it should "сorrectly merge tickets #3" in {
    val list = List(
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad(None, started, 15.minutes.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#1"), WorkLogPayLoad("Meeting".some, started, 15.minutes.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad(None, started, 15.minutes.toSeconds.toInt)),
    )

    val expected = List(
      WorklogPayloadWithTicket(TicketId("test#3"), WorkLogPayLoad(None, started, 30.minutes.toSeconds.toInt)),
      WorklogPayloadWithTicket(TicketId("test#1"), WorkLogPayLoad("Meeting".some, started, 15.minutes.toSeconds.toInt)),
    )

    merge(list) should be(expected)
  }
}
