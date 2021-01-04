package com.eg.timeTrackingHelper.service.processing

import com.eg.timeTrackingHelper.service.chain.WorkLogTransformationChain.WorkLogTransformationState
import com.eg.timeTrackingHelper.service.model.WorklogEntity
import com.eg.timeTrackingHelper.configuration.ApplicationConfig.applicationSettings.{
  minTimeFrame,
  workHoursLimit
}

private[processing] trait CalculateMinorTicketsTask extends BaseTask {

  protected def calculateMinorTickets(
    worklogs: List[WorklogEntity]
  )(implicit startedDateTime: String) =
    (state: WorkLogTransformationState) =>
      worklogs.foldLeft(state)((state, element) =>
        if (state.totalDuration < workHoursLimit)
          updateTransitionalState(
            state,
            List(buildWorklogWithTicket(element, minTimeFrame)),
            state.totalDuration + minTimeFrame
          )
        else state
      )
}
