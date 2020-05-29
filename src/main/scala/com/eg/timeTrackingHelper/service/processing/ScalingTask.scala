package com.eg.timeTrackingHelper.service.processing

import com.eg.timeTrackingHelper.configuration.ApplicationConfig.applicationSettings.{scaleFactor, workHoursLimit}
import com.eg.timeTrackingHelper.service.chain.WorkLogTransformationChain.WorkLogTransformationState

import scala.annotation.tailrec

private[processing] trait ScalingTask extends BaseTask {

  @tailrec
  final protected def scaling(state: WorkLogTransformationState): WorkLogTransformationState = {
    val scaleState = makeScaling(state)
    if (state.payloads.nonEmpty && scaleState.totalDuration < workHoursLimit)
      scaling(scaleState)
    else scaleState
  }

  private def makeScaling(state: WorkLogTransformationState): WorkLogTransformationState =
    state.payloads.foldLeft(
      state.copy(payloads = List.empty)
    )(
      (state, element) =>
        if (state.totalDuration < workHoursLimit) {
          val scaleTime = calculateTaskDuration(
            (element.payload.timeSpentSeconds * scaleFactor).toInt,
            state.totalDuration
          )
          state.copy(
            state.payloads :+ element.copy(
              payload = element.payload.copy(
                timeSpentSeconds = element.payload.timeSpentSeconds + scaleTime
              )
            ),
            state.totalDuration + scaleTime
          )
        } else
          state.copy(payloads = state.payloads :+ element)
    )
}
