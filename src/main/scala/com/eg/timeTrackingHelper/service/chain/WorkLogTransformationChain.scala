package com.eg.timeTrackingHelper.service.chain

import com.eg.timeTrackingHelper.service.chain.WorkLogTransformationChain.WorkLogTransformationState
import com.eg.timeTrackingHelper.service.model.WorklogPayloadWithTicket

class WorkLogTransformationChain(
                                  limit: Int,
                                  val tasks: List[WorkLogTransformationState => WorkLogTransformationState]
                                ) extends Chain[WorkLogTransformationState] {
  override val initialState: WorkLogTransformationState = WorkLogTransformationState()

  override def canContinue(state: WorkLogTransformationState): Boolean =
    state.totalDuration < limit
}

object WorkLogTransformationChain {

  case class WorkLogTransformationState(
                                         payloads: List[WorklogPayloadWithTicket] = List.empty,
                                         totalDuration: Int = 0
                                       )

  def apply(
             limit: Int,
             tasks: List[WorkLogTransformationState => WorkLogTransformationState]
           ): WorkLogTransformationChain =
    new WorkLogTransformationChain(
      limit,
      tasks,
    )
}
