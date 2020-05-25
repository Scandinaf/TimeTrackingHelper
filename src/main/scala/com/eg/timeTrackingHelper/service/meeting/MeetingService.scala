package com.eg.timeTrackingHelper.service.meeting

import java.time.LocalDate

import cats.syntax.option._
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
      logEntities <- IO(meetings.map(getLogEntityByMeeting).flatten)
      result <- IO(logEntities.groupBy(_.start.toLocalDate))
    } yield result

  protected[meeting] def getLogEntityByMeeting(
                                                meeting: Meeting
                                              ): Option[WorklogEntity] =
    getTicketIdBySubject(meeting.subject)
      .map(
        WorklogEntity(
          meeting.start,
          meeting.end,
          _,
          ActivityType.Major,
          meeting.subject,
        )
      )

  protected[meeting] def getTicketIdBySubject(
                                               subjectOpt: Option[String]
                                             ): Option[TicketId] =
    subjectOpt.flatMap {
      case subject if keywordMapping.excludeKeywords.exists(subject.contains(_)) => None
      case subject => getTicketIdBySubject(subject).some
    }

  protected[meeting] def getTicketIdBySubject(subject: String): TicketId =
    TicketId(
      keywordMapping.keywordMappingByTicket.find(
        tuple => tuple._2.exists(subject.contains(_))
      ).map(_._1)
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