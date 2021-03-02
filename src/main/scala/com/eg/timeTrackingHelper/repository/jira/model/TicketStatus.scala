package com.eg.timeTrackingHelper.repository.jira.model

case class TicketStatus(value: String)

object TicketStatus {

  def apply(value: String): TicketStatus =
    new TicketStatus(value.toLowerCase())

}
