package com.eg.timeTrackingHelper.service.model

object ActivityType {

  sealed trait ActivityType

  case object Major extends ActivityType

  case object Minor extends ActivityType

}
