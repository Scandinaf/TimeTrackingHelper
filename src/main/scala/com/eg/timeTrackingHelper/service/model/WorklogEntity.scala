package com.eg.timeTrackingHelper.service.model

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import com.eg.timeTrackingHelper.repository.jira.model.TicketId
import com.eg.timeTrackingHelper.service.model.ActivityType.ActivityType

import scala.concurrent.duration.Duration

case class WorklogEntity(
                          start: LocalDateTime,
                          end: LocalDateTime,
                          ticketId: TicketId,
                          activityType: ActivityType,
                          description: Option[String] = None,
                        ) {
  def duration: Duration =
    Duration(
      ChronoUnit.MINUTES.between(start, end),
      TimeUnit.MINUTES
    )
}
