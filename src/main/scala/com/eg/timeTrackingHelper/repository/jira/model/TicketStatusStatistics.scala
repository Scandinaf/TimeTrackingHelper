package com.eg.timeTrackingHelper.repository.jira.model

case class TicketStatusStatistics(user: User, statuses: Map[TicketStatus, List[DatePeriod]])
