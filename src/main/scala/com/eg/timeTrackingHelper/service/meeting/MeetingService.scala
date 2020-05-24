package com.eg.timeTrackingHelper.service.meeting

import java.time.LocalDate

import cats.effect.IO
import com.eg.timeTrackingHelper.configuration.model.KeywordMapping
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.jira.model.TicketId
import com.eg.timeTrackingHelper.repository.meeting.MeetingRepository
import com.eg.timeTrackingHelper.repository.meeting.model.Meeting
import com.eg.timeTrackingHelper.service.model.{ActivityType, WorklogEntity}

private[service] class MeetingService(
                                       meetingRepository: MeetingRepository,
                                       keywordMapping: KeywordMapping,
                                     ) {

  def getMeetingsLogEntity(
                            datePeriod: DatePeriod
                          ): IO[Map[LocalDate, List[WorklogEntity]]] =
    for {
      meetings <- meetingRepository.getTakePlaceMeetings(datePeriod)
      logEntities <- IO(meetings.map(getLogEntityByMeeting))
      result <- IO(logEntities.groupBy(_.start.toLocalDate))
    } yield result

  protected[meeting] def getLogEntityByMeeting(
                                                meeting: Meeting
                                              ): WorklogEntity =
    WorklogEntity(
      meeting.start,
      meeting.end,
      getTicketIdBySubject(meeting.subject),
      ActivityType.Major,
      meeting.subject,
    )

  protected[meeting] def getTicketIdBySubject(subject: Option[String]): TicketId =
    TicketId(
      subject.flatMap(
        subject =>
          keywordMapping.keywordMappingByTicket.find(
            tuple => tuple._2.exists(subject.contains(_))
          )
      )
        .map(_._1)
        .getOrElse(
          keywordMapping.defaultTicket
        )
    )
}

object MeetingService {
  def apply(
             meetingRepository: MeetingRepository,
             keywordMapping: KeywordMapping,
           ): MeetingService =
    new MeetingService(
      meetingRepository,
      keywordMapping,
    )
}