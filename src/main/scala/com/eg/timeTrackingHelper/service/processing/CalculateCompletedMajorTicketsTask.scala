package com.eg.timeTrackingHelper.service.processing

import com.eg.timeTrackingHelper.service.chain.WorkLogTransformationChain.WorkLogTransformationState
import com.eg.timeTrackingHelper.service.model.WorklogPayloadWithTicket
import com.eg.timeTrackingHelper.configuration.ApplicationConfig.applicationSettings.{
  minTaskTime,
  workHoursLimit
}

private[processing] trait CalculateCompletedMajorTicketsTask extends BaseTask {

  protected def calculateCompletedMajorTickets(worklogs: List[WorklogPayloadWithTicket]) =
    (state: WorkLogTransformationState) =>
      worklogs.foldLeft(state)((state, element) =>
        if (isSuitable(state, element)) calculateState(state, element)
        else state
      )

  private def isSuitable(
    state: WorkLogTransformationState,
    element: WorklogPayloadWithTicket
  ): Boolean =
    state.totalDuration < workHoursLimit &&
      element.payload.timeSpentSeconds > minTaskTime

  private def calculateState(
    state: WorkLogTransformationState,
    element: WorklogPayloadWithTicket
  ): WorkLogTransformationState = {
    val taskDuration = calculateTaskDuration(element.payload.timeSpentSeconds, state.totalDuration)
    updateTransitionalState(
      state,
      List(element.copy(payload = element.payload.copy(timeSpentSeconds = taskDuration))),
      state.totalDuration + taskDuration
    )
  }
}
