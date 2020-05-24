package com.eg.timeTrackingHelper.service

import java.time.{LocalDateTime, Month}

import com.eg.timeTrackingHelper.repository.jira.model.TicketId
import com.eg.timeTrackingHelper.service.model.{ActivityType, WorklogEntity}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HierarchicalWorklogBuilderSpec extends AnyFlatSpec
  with Matchers
  with EitherValues
  with HierarchicalWorklogBuilder {

  "HierarchicalWorklogBuilder" should "return correctly list of non-conflicting worklogs" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 20, 14, 30, 10, 569000000)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(1),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusHours(1),
      end = start.plusHours(2),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val entity3 = WorklogEntity(
      start = start.plusHours(2),
      end = start.plusHours(3),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )
    val list = List(
      entity3,
      entity1,
      entity2,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      entity1,
      entity2,
      entity3,
    ))
  }

  it should "return correctly list of non-conflicting worklogs with overflow" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 19, 14, 30, 10, 569000000)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusDays(1).plusHours(1),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusDays(1).plusHours(1),
      end = start.plusDays(2).plusHours(2),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val list = List(
      entity2,
      entity1,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      entity1,
      entity2,
    ))
  }

  it should "return correctly list of non-conflicting worklogs with gaps" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 20, 14, 30, 10, 569000000)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(1),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusHours(2),
      end = start.plusHours(3),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val entity3 = WorklogEntity(
      start = start.plusHours(5),
      end = start.plusHours(6),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )
    val list = List(
      entity3,
      entity1,
      entity2,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      entity1,
      entity2,
      entity3,
    ))
  }

  it should "return correctly list of conflicting worklogs(pyramid)" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 20, 10, 30, 10, 569000000)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(8),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusHours(1),
      end = start.plusHours(7),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val entity3 = WorklogEntity(
      start = start.plusHours(2),
      end = start.plusHours(6),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )
    val list = List(
      entity3,
      entity1,
      entity2,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      WorklogEntity(
        start = start,
        end = start.plusHours(1),
        ticketId = TicketId("test#1"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(1),
        end = start.plusHours(2),
        ticketId = TicketId("test#2"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(2),
        end = start.plusHours(6),
        ticketId = TicketId("test#3"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(6),
        end = start.plusHours(7),
        ticketId = TicketId("test#2"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(7),
        end = start.plusHours(8),
        ticketId = TicketId("test#1"),
        ActivityType.Major,
      )
    ))
  }

  it should "return correctly list of conflicting worklogs(pyramid) with overflow" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 20, 10, 30, 10, 569000000)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(8),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusHours(1),
      end = start.plusHours(7),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val entity3 = WorklogEntity(
      start = start.plusHours(2),
      end = start.plusDays(1).plusHours(6),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )
    val list = List(
      entity3,
      entity1,
      entity2,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      WorklogEntity(
        start = start,
        end = start.plusHours(1),
        ticketId = TicketId("test#1"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(1),
        end = start.plusHours(2),
        ticketId = TicketId("test#2"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(2),
        end = start.plusDays(1).plusHours(6),
        ticketId = TicketId("test#3"),
        ActivityType.Major,
      ),
    ))
  }

  it should "return correctly list of conflicting worklogs" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 20, 10, 30, 10, 569000000)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(8),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusHours(2),
      end = start.plusHours(4),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val entity3 = WorklogEntity(
      start = start.plusHours(3),
      end = start.plusHours(6),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )
    val entity4 = WorklogEntity(
      start = start.plusHours(4),
      end = start.plusHours(7),
      ticketId = TicketId("test#4"),
      ActivityType.Major,
    )
    val list = List(
      entity3,
      entity1,
      entity4,
      entity2,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      WorklogEntity(
        start = start,
        end = start.plusHours(2),
        ticketId = TicketId("test#1"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(2),
        end = start.plusHours(3),
        ticketId = TicketId("test#2"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(3),
        end = start.plusHours(4),
        ticketId = TicketId("test#3"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(4),
        end = start.plusHours(7),
        ticketId = TicketId("test#4"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(7),
        end = start.plusHours(8),
        ticketId = TicketId("test#1"),
        ActivityType.Major,
      )
    ))
  }

  it should "return correctly list of conflicting worklogs with overflow" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 20, 10, 30, 10, 569000000)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(8),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusHours(2),
      end = start.plusHours(4),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val entity3 = WorklogEntity(
      start = start.plusHours(3),
      end = start.plusDays(1).plusHours(6),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )
    val list = List(
      entity3,
      entity1,
      entity2,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      WorklogEntity(
        start = start,
        end = start.plusHours(2),
        ticketId = TicketId("test#1"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(2),
        end = start.plusHours(3),
        ticketId = TicketId("test#2"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(3),
        end = start.plusDays(1).plusHours(6),
        ticketId = TicketId("test#3"),
        ActivityType.Major,
      ),
    ))
  }

  it should "сorrectly process scenario #1" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 20, 10, 0, 0, 0x0)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusHours(4),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusHours(2),
      end = start.plusHours(3),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val entity3 = WorklogEntity(
      start = start.plusHours(6),
      end = start.plusHours(10),
      ticketId = TicketId("test#3"),
      ActivityType.Major,
    )
    val entity4 = WorklogEntity(
      start = start.plusHours(6).plusMinutes(30),
      end = start.plusHours(8),
      ticketId = TicketId("test#4"),
      ActivityType.Major,
    )
    val entity5 = WorklogEntity(
      start = start.plusHours(7),
      end = start.plusHours(11),
      ticketId = TicketId("test#5"),
      ActivityType.Major,
    )
    val list = List(
      entity3,
      entity1,
      entity5,
      entity2,
      entity4,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      WorklogEntity(
        start = start,
        end = start.plusHours(2),
        ticketId = TicketId("test#1"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(2),
        end = start.plusHours(3),
        ticketId = TicketId("test#2"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(3),
        end = start.plusHours(4),
        ticketId = TicketId("test#1"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(6),
        end = start.plusHours(6).plusMinutes(30),
        ticketId = TicketId("test#3"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(6).plusMinutes(30),
        end = start.plusHours(7),
        ticketId = TicketId("test#4"),
        ActivityType.Major,
      ),
      WorklogEntity(
        start = start.plusHours(7),
        end = start.plusHours(11),
        ticketId = TicketId("test#5"),
        ActivityType.Major,
      )
    ))
  }

  it should "сorrectly process scenario #2" in {
    val start = LocalDateTime.of(2020, Month.JANUARY, 20, 10, 0, 0, 0x0)
    val entity1 = WorklogEntity(
      start = start,
      end = start.plusDays(1).plusHours(4),
      ticketId = TicketId("test#1"),
      ActivityType.Major,
    )
    val entity2 = WorklogEntity(
      start = start.plusDays(1).plusHours(5),
      end = start.plusDays(2).plusHours(8),
      ticketId = TicketId("test#2"),
      ActivityType.Major,
    )
    val list = List(
      entity2,
      entity1,
    )
    val result = toHierarchicalWorklogs(list)
    result.minor shouldBe empty
    result.major should be(List(
      entity1,
      entity2,
    ))
  }
}
