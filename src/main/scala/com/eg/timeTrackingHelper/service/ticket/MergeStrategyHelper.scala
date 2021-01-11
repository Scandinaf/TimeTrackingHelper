package com.eg.timeTrackingHelper.service.ticket

import java.text.SimpleDateFormat
import java.time.LocalDateTime

import com.eg.timeTrackingHelper.repository.jira.model.{TicketId, TicketStatus}
import com.eg.timeTrackingHelper.service.model.ActivityType.ActivityType
import com.eg.timeTrackingHelper.service.model.{ActivityType, Assignee, TicketState, WorklogEntity}
import com.eg.timeTrackingHelper.utils.DateTimeHelper.StrToLocalDateTime

private[ticket] trait MergeStrategyHelper {

  protected def defaultEnd: LocalDateTime

  protected implicit def jiraDateFormat: SimpleDateFormat

  protected def majorTicketStatuses: Set[TicketStatus]

  protected implicit class TicketStateCompanion(ticketState: TicketState) {
    def merge(
      getWorklogEntities: LocalDateTime => List[WorklogEntity],
      newStatus: Option[TicketStatus] = None,
      newAssignee: Option[Assignee] = None
    )(implicit created: String): Either[Throwable, TicketState] =
      created.toLDT.map { created =>
        ticketState.copy(
          assignee = newAssignee.orElse(ticketState.assignee),
          status = newStatus.orElse(ticketState.status),
          worklogEntities = getWorklogEntities(created)
        )
      }
  }

  protected implicit class WorklogEntitiesCompanion(worklogEntities: List[WorklogEntity]) {
    def update(end: LocalDateTime): List[WorklogEntity] =
      worklogEntities.init :+
        worklogEntities.last
          .copy(end = end)

    def add(
      start: LocalDateTime,
      ticketId: TicketId,
      activityType: ActivityType
    ): List[WorklogEntity] =
      worklogEntities :+
        WorklogEntity(
          start = start,
          end = defaultEnd,
          ticketId = ticketId,
          activityType = activityType
        )
  }

  protected def getActivityType(status: TicketStatus): ActivityType =
    if (majorTicketStatuses.contains(status))
      ActivityType.Major
    else
      ActivityType.Minor
}
