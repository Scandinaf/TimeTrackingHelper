package com.eg.timeTrackingHelper.service.model

import com.allantl.jira4s.v2.domain.WorkLogPayLoad
import com.eg.timeTrackingHelper.repository.jira.model.TicketId

case class WorklogPayloadWithTicket(
                                     ticketId: TicketId,
                                     payload: WorkLogPayLoad
                                   )
