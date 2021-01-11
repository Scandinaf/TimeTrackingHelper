package com.eg.timeTrackingHelper.service.processing

import com.allantl.jira4s.v2.domain.WorkLogPayLoad
import com.eg.timeTrackingHelper.configuration.ApplicationConfig.applicationSettings.{
  minTimeFrame,
  workHoursLimit
}
import com.eg.timeTrackingHelper.service.chain.WorkLogTransformationChain.WorkLogTransformationState
import com.eg.timeTrackingHelper.service.model.{WorklogEntity, WorklogPayloadWithTicket}
import com.eg.timeTrackingHelper.utils.DateTimeHelper

private[processing] trait BaseTask {

  protected def updateTransitionalState(
    state: WorkLogTransformationState,
    payloads: List[WorklogPayloadWithTicket],
    totalDuration: Int
  ): WorkLogTransformationState =
    state.copy(state.payloads ::: payloads, totalDuration)

  protected def checkOverflow(duration: Int, totalDuration: Int): Int =
    if (totalDuration + duration > workHoursLimit)
      workHoursLimit - totalDuration
    else
      duration

  protected def buildWorklogWithTicket(entity: WorklogEntity, timeSpentInSeconds: Int)(implicit
    startedDateTime: String
  ): WorklogPayloadWithTicket =
    WorklogPayloadWithTicket(
      entity.ticketId,
      WorkLogPayLoad(entity.description, startedDateTime, timeSpentInSeconds)
    )

  protected def calculateTaskDuration(duration: Int, totalDuration: Int): Int =
    checkOverflow(DateTimeHelper.scaleToTimeFrame(duration, minTimeFrame), totalDuration)
}
