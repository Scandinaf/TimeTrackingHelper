package com.eg.timeTrackingHelper.service.ticket

import cats.syntax.either._
import cats.syntax.option._
import com.eg.timeTrackingHelper.repository.jira.model.{TicketId, TicketStatus}
import com.eg.timeTrackingHelper.service.exception.IncompleteTicketInformationException
import com.eg.timeTrackingHelper.service.model.{Assignee, TicketState}

private[ticket] trait MergeStrategy extends MergeStrategyHelper {

  protected def ticketStatuses: Set[TicketStatus]

  protected def currentUser: Assignee

  protected[ticket] def mergeTicketState(
    oldTicketState: TicketState,
    newTicketState: TicketState
  )(implicit ticketId: TicketId, created: String): Either[Throwable, TicketState] =
    newTicketState match {
      case TicketState(None, Some(status), _) =>
        mergeStatus(status, oldTicketState)

      case TicketState(Some(assignee), None, _) =>
        mergeAssignee(assignee, oldTicketState)

      case TicketState(Some(assignee), Some(status), _)
          if oldTicketState.assignee.isDefined &&
            assignee == currentUser &&
            ticketStatuses.contains(status) =>
        mergeStatus(status, oldTicketState)
          .flatMap(mergeAssignee(assignee, _))

      case TicketState(Some(assignee), Some(status), _) =>
        mergeAssignee(assignee, oldTicketState)
          .flatMap(mergeStatus(status, _))

      case TicketState(None, None, _) =>
        oldTicketState.asRight
    }

  protected[ticket] def mergeAssignee(newAssignee: Assignee, oldTicketState: TicketState)(implicit
    ticketId: TicketId,
    created: String
  ): Either[Throwable, TicketState] =
    oldTicketState match {
      case TicketState(Some(assignee), Some(status), worklogEntities)
          if assignee == currentUser &&
            ticketStatuses.contains(status) =>
        oldTicketState.merge(worklogEntities.update(_), newAssignee = newAssignee.some)

      case TicketState(Some(_), Some(status), worklogEntities)
          if newAssignee == currentUser &&
            ticketStatuses.contains(status) =>
        oldTicketState.merge(
          worklogEntities.add(_, ticketId, getActivityType(status)),
          newAssignee = newAssignee.some
        )

      case _ =>
        oldTicketState.copy(assignee = newAssignee.some).asRight
    }

  protected[ticket] def mergeStatus(newStatus: TicketStatus, oldTicketState: TicketState)(implicit
    ticketId: TicketId,
    created: String
  ): Either[Throwable, TicketState] =
    oldTicketState match {
      case TicketState(None, _, _) if ticketStatuses.contains(newStatus) =>
        IncompleteTicketInformationException().asLeft

      case TicketState(Some(assignee), None, worklogEntities)
          if assignee == currentUser
            && ticketStatuses.contains(newStatus) =>
        oldTicketState.merge(
          worklogEntities.add(_, ticketId, getActivityType(newStatus)),
          newStatus.some
        )

      case TicketState(Some(assignee), Some(oldStatus), worklogEntities)
          if assignee == currentUser &&
            ticketStatuses.contains(oldStatus) &&
            ticketStatuses.contains(newStatus) =>
        oldTicketState.merge(
          created =>
            worklogEntities
              .update(created)
              .add(created, ticketId, getActivityType(newStatus)),
          newStatus.some
        )

      case TicketState(Some(assignee), Some(oldStatus), worklogEntities)
          if assignee == currentUser &&
            ticketStatuses.contains(oldStatus) =>
        oldTicketState
          .merge(worklogEntities.update(_), newStatus.some)

      case TicketState(Some(assignee), Some(_), worklogEntities)
          if assignee == currentUser &&
            ticketStatuses.contains(newStatus) =>
        oldTicketState.merge(
          worklogEntities.add(_, ticketId, getActivityType(newStatus)),
          newStatus.some
        )

      case _ =>
        oldTicketState.copy(status = newStatus.some).asRight
    }
}
