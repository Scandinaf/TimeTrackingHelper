package com.eg.timeTrackingHelper.repository.meeting.outlook

import java.net.URI

import cats.effect.IO
import cats.implicits._
import com.eg.timeTrackingHelper.configuration.model.OutlookConfig
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.meeting.MeetingRepository
import com.eg.timeTrackingHelper.repository.meeting.model.Meeting
import com.eg.timeTrackingHelper.utils.converter.Converter.{appointmentToMeeting, convertTo}
import microsoft.exchange.webservices.data.core.service.item.Appointment
import shapeless.syntax.std.product._

private[meeting] class OutlookMeetingRepository(config: OutlookConfig)
  extends MeetingRepository
    with OutlookExchangeService {
  override def getMeetings(datePeriod: DatePeriod): IO[List[Meeting]] =
    for {
      appointments <- findOutlookAppointments(datePeriod)
      meetings <- IO.fromEither(appointments.traverse(convertTo[Appointment, Meeting]))
    } yield meetings

  override protected def getSettings: (String, String, URI) = config.productElements.tupled
}

object OutlookMeetingRepository {
  def apply(config: OutlookConfig): OutlookMeetingRepository =
    new OutlookMeetingRepository(config)
}