package com.eg.timeTrackingHelper.service.ticket

import java.text.SimpleDateFormat
import java.time.{LocalDate, LocalDateTime}

import cats.effect.IO
import cats.implicits._
import com.allantl.jira4s.v2.domain.enums.SearchExpand
import com.allantl.jira4s.v2.domain.{HistoryItem, Issue}
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.jira.JiraRepository
import com.eg.timeTrackingHelper.repository.jira.model.{
  TicketId,
  TicketStatus,
  TicketStatusStatistics
}
import com.eg.timeTrackingHelper.service.model.{Assignee, TicketState, WorklogEntity}
import com.eg.timeTrackingHelper.utils.DateTimeHelper.{DatePeriodCompanion, DateToLocalDateTime}

private[service] class TicketService(
  jiraRepository: JiraRepository,
  protected val currentUser: Assignee,
  protected val majorTicketStatuses: Set[TicketStatus],
  minorTicketStatuses: Set[TicketStatus]
) extends MergeStrategy {

  protected val defaultEnd: LocalDateTime = LocalDateTime.now().plusDays(1)
  protected implicit val jiraDateFormat: SimpleDateFormat = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  )
  protected val ticketStatuses = majorTicketStatuses ++ minorTicketStatuses
  private val nonExistentFieldName = "NonExistent"

  def getTicketsLogEntity(datePeriod: DatePeriod): IO[Map[LocalDate, List[WorklogEntity]]] =
    for {
      searchResult <- jiraRepository.getMyTicketsByDatePeriod(
        datePeriod,
        ticketStatuses,
        fields = Set(nonExistentFieldName).some,
        expand = Set[SearchExpand](SearchExpand.Changelog).some
      )
      worklogEntities <- searchResult.issues.traverse(getLogEntitiesByTicket)
      rangeMap <- IO(buildRangeMap(worklogEntities.flatten))
    } yield rangeMap

  private def buildRangeMap(
    worklogEntities: List[WorklogEntity]
  ): Map[LocalDate, List[WorklogEntity]] =
    worklogEntities.foldMap(worklogEntity =>
      DatePeriod(
        worklogEntity.start.toLocalDate,
        worklogEntity.end.toLocalDate.plusDays(1)
      ).toListLocalDate
        .map((_, List(worklogEntity)))
        .toMap
    )

  private def getLogEntitiesByTicket(ticket: Issue): IO[List[WorklogEntity]] = {
    val ticketId = TicketId(ticket.key)
    ticket.changelog
      .map(
        _.histories.foldLeft(TicketState().asRight[Throwable])((ticketState, history) =>
          ticketState.flatMap(
            mergeTicketState(_, getLogEntitiesByHistoryItems(history.items))(
              ticketId,
              history.created
            )
          )
        )
      )
      .map(
        _.fold(
          _ =>
            for {
              ticketStatusStatisticsList <- jiraRepository.getTicketStatusStatistics(ticketId)
            } yield ticketStatusStatisticsToWorklogEntities(ticketStatusStatisticsList, ticketId),
          ticketState => IO.pure(ticketState.worklogEntities)
        )
      )
      .getOrElse(IO.pure(List.empty))
  }

  private def ticketStatusStatisticsToWorklogEntities(
    ticketStatusStatisticsList: List[TicketStatusStatistics],
    ticketId: TicketId
  ): List[WorklogEntity] =
    ticketStatusStatisticsList
      .find(_.user.userName == currentUser.userName)
      .map(ticketStatusStatisticsToWorklogEntity(_, ticketId))
      .getOrElse(List.empty)

  private def ticketStatusStatisticsToWorklogEntity(
    ticketStatusStatistics: TicketStatusStatistics,
    ticketId: TicketId
  ): List[WorklogEntity] =
    ticketStatusStatistics.statuses.view
      .filterKeys(ticketStatuses.contains)
      .toList
      .flatMap(tuple => {
        val (ticketStatus, datePeriods) = tuple
        datePeriods.map(datePeriod =>
          WorklogEntity(
            start = datePeriod.start.toLocalDateTime,
            end = datePeriod.end.toLocalDateTime,
            ticketId = ticketId,
            activityType = getActivityType(ticketStatus)
          )
        )
      })

  private def getLogEntitiesByHistoryItems(historyItems: List[HistoryItem]): TicketState =
    historyItems.foldLeft(TicketState())((ticketState, historyItem) =>
      historyItem match {
        case HistoryItem("assignee", "jira", _, _, _, Some(userName), _) =>
          ticketState.copy(Assignee(userName).some)
        case HistoryItem("status", "jira", _, _, _, _, Some(status)) =>
          ticketState.copy(status = TicketStatus(status).some)
        case _ =>
          ticketState
      }
    )
}

object TicketService {
  def apply(
    jiraRepository: JiraRepository,
    currentUser: Assignee,
    majorTicketStatuses: Set[TicketStatus],
    minorTicketStatuses: Set[TicketStatus]
  ): TicketService =
    new TicketService(
      jiraRepository,
      currentUser: Assignee,
      majorTicketStatuses,
      minorTicketStatuses
    )
}
