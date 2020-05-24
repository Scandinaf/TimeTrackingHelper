package com.eg.timeTrackingHelper.repository.jira

import cats.effect.IO
import com.allantl.jira4s.v2.domain._
import com.allantl.jira4s.v2.domain.enums.SearchExpand
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.jira.model.{TicketId, TicketStatus, TicketStatusStatistics}

private[jira] trait Repository {
  def getTickets(
                  ticketIds: Set[TicketId],
                  fields: Option[Set[String]] = None
                ): IO[SearchResults]

  def getTicket(
                 ticketId: TicketId,
                 fields: List[String] = List("*all")
               ): IO[Issue]

  def addWorkLog(
                  ticketId: TicketId,
                  workLogPayLoad: WorkLogPayLoad,
                  adjustEstimate: AdjustEstimate
                ): IO[WorkLogCreateResponse]

  def getTicketStatusStatistics(
                                 ticketId: TicketId
                               ): IO[List[TicketStatusStatistics]]

  def getMyTicketsByDatePeriod(
                                datePeriod: DatePeriod,
                                statuses: Set[TicketStatus],
                                fields: Option[Set[String]] = None,
                                expand: Option[Set[SearchExpand]] = None,
                                maxResults: Int = 200,
                              ): IO[SearchResults]
}
