package com.eg.timeTrackingHelper.service.processing

private[service] trait WorklogDataProcessing
  extends CalculateCompletedMajorTicketsTask
    with CalculateMajorTicketsTask
    with CalculateMinorTicketsTask
    with ScalingTask {

}
