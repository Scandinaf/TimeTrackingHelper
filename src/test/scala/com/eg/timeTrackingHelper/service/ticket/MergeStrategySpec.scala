package com.eg.timeTrackingHelper.service.ticket

import java.text.{ParseException, SimpleDateFormat}
import java.time.{LocalDateTime, Month}

import cats.syntax.option._
import com.eg.timeTrackingHelper.repository.jira.model.{TicketId, TicketStatus}
import com.eg.timeTrackingHelper.service.exception.IncompleteTicketInformationException
import com.eg.timeTrackingHelper.service.model.{ActivityType, Assignee, TicketState, WorklogEntity}
import com.eg.timeTrackingHelper.utils.DateTimeHelper.StrToLocalDateTime
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MergeStrategySpec extends AnyFlatSpec
  with MergeStrategy
  with Matchers
  with EitherValues {

  implicit val ticketId = TicketId("test-ticket")
  implicit val created: String = "2020-01-14T11:27:57.623+0000"
  override protected implicit val jiraDateFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  val inProgressTicketStatus = TicketStatus("In Progress")
  val readyForReviewTicketStatus = TicketStatus("Ready for Review")
  val createdLocalDateTime = created.toLDT.right.value
  override protected val majorTicketStatuses: Set[TicketStatus] = Set(inProgressTicketStatus)
  protected val minorTicketStatuses: Set[TicketStatus] = Set(readyForReviewTicketStatus)
  override protected val ticketStatuses: Set[TicketStatus] = majorTicketStatuses ++ minorTicketStatuses
  override protected val currentUser: Assignee = Assignee("testUser")
  override protected val defaultEnd: LocalDateTime = LocalDateTime.of(
    2020,
    Month.JANUARY,
    16,
    17,
    30,
    41,
    749000000
  )

  "MergeStrategy" should "return exception if assignee undefined and we operate the major or minor status" in {
    mergeStatus(inProgressTicketStatus, TicketState())
      .left.value should be(IncompleteTicketInformationException())
    mergeStatus(readyForReviewTicketStatus, TicketState())
      .left.value should be(IncompleteTicketInformationException())
  }

  it should "return new ticketState if assignee undefined and we not operate the major or minor status" in {
    val openTicketStatus = TicketStatus("Open")
    mergeStatus(openTicketStatus, TicketState())
      .right.value should be(TicketState(status = openTicketStatus.some))
    val testTicketStatus = TicketStatus("TestStatus")
    mergeStatus(testTicketStatus, TicketState())
      .right.value should be(TicketState(status = testTicketStatus.some))
  }

  it should "return exception if couldn't parse date" in {
    implicit val created: String = "20asd20-01-14T11:27:57.623+0000"
    mergeStatus(inProgressTicketStatus, TicketState(currentUser.some))
      .left.value shouldBe a[ParseException]
  }

  it should "not return exception if date incorrect and status not a minor or major" in {
    implicit val created: String = "20asd20-01-14T11:27:57.623+0000"
    val openTicketStatus = TicketStatus("Open")
    mergeStatus(openTicketStatus, TicketState(currentUser.some))
      .right.value should be(TicketState(currentUser.some, openTicketStatus.some))
  }

  it should "correctly merge TicketStates" in {
    mergeTicketState(TicketState(), TicketState())
      .right.value should be(TicketState())

    val openTicketStatus = TicketStatus("Open")
    mergeTicketState(TicketState(), TicketState(Assignee("user").some, openTicketStatus.some))
      .right.value should be(TicketState(Assignee("user").some, openTicketStatus.some))

    mergeTicketState(TicketState(), TicketState(currentUser.some, inProgressTicketStatus.some))
      .right.value should be(TicketState(currentUser.some, inProgressTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ))))

    mergeTicketState(
      TicketState(Assignee("user").some, openTicketStatus.some),
      TicketState(Assignee("user#1").some, inProgressTicketStatus.some))
      .right.value should be(TicketState(Assignee("user#1").some, inProgressTicketStatus.some))
  }

  it should "сorrectly process scenario #1" in {
    val step1Result = mergeTicketState(TicketState(), TicketState(currentUser.some))
      .right.value
    step1Result should be(TicketState(currentUser.some))

    val step2Result = mergeTicketState(step1Result, TicketState(status = inProgressTicketStatus.some))
      .right.value
    step2Result should be(TicketState(currentUser.some, inProgressTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ))))

    val step3Result = mergeTicketState(step2Result, TicketState(status = readyForReviewTicketStatus.some))
      .right.value
    step3Result should be(TicketState(currentUser.some, readyForReviewTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      ),
    )))

    val step4Result = mergeTicketState(step3Result, TicketState(status = TicketStatus("Ready for cit1").some))
      .right.value
    step4Result should be(TicketState(currentUser.some, TicketStatus("Ready for cit1").some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      ),
    )))
  }

  it should "сorrectly process scenario #2" in {
    val step1Result = mergeTicketState(TicketState(), TicketState(currentUser.some, inProgressTicketStatus.some))
      .right.value
    step1Result should be(TicketState(currentUser.some, inProgressTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ))))

    val step2Result = mergeTicketState(step1Result, TicketState(Assignee("user#1").some))
      .right.value
    step2Result should be(TicketState(Assignee("user#1").some, inProgressTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ))))

    val step3Result = mergeTicketState(step2Result, TicketState(currentUser.some, readyForReviewTicketStatus.some))
      .right.value
    step3Result should be(TicketState(currentUser.some, readyForReviewTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      ),
    )))

    val step4Result = mergeTicketState(step3Result, TicketState(currentUser.some, TicketStatus("Ready for cit1").some))
      .right.value
    step4Result should be(TicketState(currentUser.some, TicketStatus("Ready for cit1").some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      ),
    )))
  }

  it should "сorrectly process scenario #3" in {
    val step1Result = mergeTicketState(TicketState(), TicketState(currentUser.some, inProgressTicketStatus.some))
      .right.value
    step1Result should be(TicketState(currentUser.some, inProgressTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ))))

    val step2Result = mergeTicketState(step1Result, TicketState(Assignee("user#1").some, readyForReviewTicketStatus.some))
      .right.value
    step2Result should be(TicketState(Assignee("user#1").some, readyForReviewTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ))))

    val step3Result = mergeTicketState(step2Result, TicketState(currentUser.some, readyForReviewTicketStatus.some))
      .right.value
    step3Result should be(TicketState(currentUser.some, readyForReviewTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      )
    )))

    val step4Result = mergeTicketState(step3Result, TicketState(status = inProgressTicketStatus.some))
      .right.value
    step4Result should be(TicketState(currentUser.some, inProgressTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Major
      )
    )))

    val step5Result = mergeTicketState(step4Result, TicketState(Assignee("user#1").some, readyForReviewTicketStatus.some))
      .right.value
    step5Result should be(TicketState(Assignee("user#1").some, readyForReviewTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      )
    )))
  }

  it should "сorrectly process scenario #4" in {
    val step1Result = mergeTicketState(TicketState(), TicketState(status = TicketStatus("Open").some))
      .right.value
    step1Result should be(TicketState(status = TicketStatus("Open").some))

    val step2Result = mergeTicketState(step1Result, TicketState(Assignee("user#1").some, inProgressTicketStatus.some))
      .right.value
    step2Result should be(TicketState(Assignee("user#1").some, inProgressTicketStatus.some))

    val step3Result = mergeTicketState(step2Result, TicketState(status = readyForReviewTicketStatus.some))
      .right.value
    step3Result should be(TicketState(Assignee("user#1").some, readyForReviewTicketStatus.some))

    val step4Result = mergeTicketState(step3Result, TicketState(currentUser.some, inProgressTicketStatus.some))
      .right.value
    step4Result should be(TicketState(currentUser.some, inProgressTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Major
      )
    )))

    val step5Result = mergeTicketState(step4Result, TicketState(status = readyForReviewTicketStatus.some))
      .right.value
    step5Result should be(TicketState(currentUser.some, readyForReviewTicketStatus.some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = defaultEnd,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      )
    )))

    val step6Result = mergeTicketState(step5Result, TicketState(status = TicketStatus("Close").some))
      .right.value
    step6Result should be(TicketState(currentUser.some, TicketStatus("Close").some, List(
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Major
      ),
      WorklogEntity(
        start = createdLocalDateTime,
        end = createdLocalDateTime,
        ticketId = ticketId,
        activityType = ActivityType.Minor
      )
    )))
  }
}