package com.eg.timeTrackingHelper.service.processing

import cats.syntax.option._
import com.eg.timeTrackingHelper.service.chain.WorkLogTransformationChain.WorkLogTransformationState
import com.eg.timeTrackingHelper.service.model.WorklogEntity
import com.eg.timeTrackingHelper.utils.DateTimeHelper
import com.eg.timeTrackingHelper.configuration.ApplicationConfig.applicationSettings.{
  minTimeFrame,
  workHoursLimit
}

private[processing] trait CalculateMajorTicketsTask extends BaseTask {

  protected def calculateMajorTickets(
    startInPastTicket: Option[WorklogEntity],
    endInFutureTicket: Option[WorklogEntity]
  )(implicit startedDateTime: String) =
    (state: WorkLogTransformationState) =>
      List(startInPastTicket, endInFutureTicket).flatten match {
        case worklogP :: worklogF :: Nil =>
          calculateState(state, worklogF, worklogP)
        case head :: Nil =>
          updateTransitionalState(
            state,
            List(buildWorklogWithTicket(head, workHoursLimit - state.totalDuration)),
            workHoursLimit
          )
        case _ => state
      }

  private def calculateState(
    state: WorkLogTransformationState,
    worklogF: WorklogEntity,
    worklogP: WorklogEntity
  )(implicit startedDateTime: String) = {
    val timeLeft = workHoursLimit - state.totalDuration
    val worklogFTime = DateTimeHelper.scaleToTimeFrame(timeLeft / 2, minTimeFrame)
    val worklogPTime = timeLeft - worklogFTime
    updateTransitionalState(
      state,
      List(
        buildWorklogWithTicket(worklogF, worklogFTime).some,
        if (worklogPTime > 0) buildWorklogWithTicket(worklogP, worklogPTime).some else None
      ).flatten,
      workHoursLimit
    )
  }
}
