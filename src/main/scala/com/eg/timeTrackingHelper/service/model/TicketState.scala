package com.eg.timeTrackingHelper.service.model

import com.eg.timeTrackingHelper.repository.jira.model.TicketStatus

private[service] case class TicketState(
  assignee: Option[Assignee] = None,
  status: Option[TicketStatus] = None,
  worklogEntities: List[WorklogEntity] = List.empty
)
