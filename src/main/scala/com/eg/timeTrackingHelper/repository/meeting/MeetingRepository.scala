package com.eg.timeTrackingHelper.repository.meeting

import cats.effect.IO
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.meeting.model.Meeting

trait MeetingRepository {
  def getMeetings(datePeriod: DatePeriod): IO[List[Meeting]]

  def getTakePlaceMeetings(datePeriod: DatePeriod): IO[List[Meeting]] =
    getMeetings(datePeriod).map(_.filter(_.isTakePlace))
}
