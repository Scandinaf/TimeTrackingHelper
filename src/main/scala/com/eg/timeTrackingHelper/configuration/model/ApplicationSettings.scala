package com.eg.timeTrackingHelper.configuration.model

case class ApplicationSettings(
  minTimeFrame: Int,
  minTaskTime: Int,
  workHoursLimit: Int,
  scaleFactor: Double,
  majorTicketStatuses: Set[String],
  minorTicketStatuses: Set[String],
  keywordMapping: KeywordMapping
)
