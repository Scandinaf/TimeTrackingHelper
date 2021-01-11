package com.eg.timeTrackingHelper.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.effect.{ContextShift, IO}
import cats.syntax.option._
import com.eg.timeTrackingHelper.configuration.ApplicationConfig.applicationSettings.workHoursLimit
import com.eg.timeTrackingHelper.model._
import com.eg.timeTrackingHelper.service.WorklogPayloadBuilder.TaskState
import com.eg.timeTrackingHelper.service.chain.WorkLogTransformationChain
import com.eg.timeTrackingHelper.service.model.{
  HierarchicalWorklog,
  WorklogEntity,
  WorklogPayloadWithTicket
}
import com.eg.timeTrackingHelper.service.processing.WorklogDataProcessing
import com.eg.timeTrackingHelper.utils.DateTimeHelper
import com.eg.timeTrackingHelper.utils.DateTimeHelper.DatePeriodCompanion
import fs2.Stream

private[service] trait WorklogPayloadBuilder
    extends HierarchicalWorklogBuilder
    with WorklogDataProcessing {

  implicit def contextShift: ContextShift[IO]

  protected val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZ")

  protected def toWorklogPayloads(
    WorklogEntityMap: Map[LocalDate, List[WorklogEntity]],
    datePeriod: DatePeriod
  ): Stream[IO, List[WorklogPayloadWithTicket]] =
    Stream
      .emits(datePeriod.toListLocalDate)
      .covary[IO]
      .mapAsyncUnordered(2)(localDate =>
        IO((localDate, toHierarchicalWorklogs(WorklogEntityMap.getOrElse(localDate, List.empty))))
      )
      .mapAsyncUnordered(2)(tuple => IO(buildWorkLogPayLoads(tuple._1, tuple._2)))

  protected def buildWorkLogPayLoads(
    localDate: LocalDate,
    hierarchicalWorklog: HierarchicalWorklog
  ): List[WorklogPayloadWithTicket] = {
    implicit val startedDateTime: String = localDate
      .atStartOfDay()
      .plusHours(10)
      .atZone(DateTimeHelper.defaultZoneId)
      .format(dateTimeFormatter)

    val map = hierarchicalWorklog.major.groupBy(entity =>
      TaskState.buildKey(entity.start.toLocalDate, entity.end.toLocalDate, localDate)
    )

    merge(
      WorkLogTransformationChain(
        workHoursLimit,
        List(
          calculateCompletedMajorTickets(
            getCompletedMajorTickets(
              map
                .get(TaskState.Completed)
                .getOrElse(List.empty)
            )
          ),
          calculateMajorTickets(
            map.get(TaskState.StartInPast).flatMap(_.lastOption),
            map.get(TaskState.EndInFuture).flatMap(_.headOption)
          ),
          calculateMinorTickets(hierarchicalWorklog.minor),
          scaling
        )
      ).execute.payloads
    )
  }

  protected def merge(list: List[WorklogPayloadWithTicket]): List[WorklogPayloadWithTicket] = {
    list
      .groupBy(_.ticketId)
      .values
      .toList
      .collect {
        case head :: Nil => head
        case list @ head :: _ =>
          head.copy(payload =
            head.payload.copy(
              comment = list
                .flatMap(_.payload.comment)
                .some
                .filter(_.nonEmpty)
                .map(_.mkString(",")),
              timeSpentSeconds = list.map(_.payload.timeSpentSeconds).sum
            )
          )
      }
      .sortBy(_.ticketId.value)
  }

  private def getCompletedMajorTickets(
    worklogs: List[WorklogEntity]
  )(implicit startDateTime: String): List[WorklogPayloadWithTicket] =
    merge(
      worklogs.map(worklogEntity =>
        buildWorklogWithTicket(worklogEntity, worklogEntity.duration.toSeconds.toInt)
      )
    ).sortBy(_.payload.timeSpentSeconds)
}

object WorklogPayloadBuilder {

  object TaskState extends Enumeration {
    type TaskState = Value
    val StartInPast, EndInFuture, Completed = Value

    def buildKey(
      taskStartDate: LocalDate,
      taskEndDate: LocalDate,
      localDate: LocalDate
    ): TaskState =
      if (taskStartDate.isEqual(localDate) && taskEndDate.isEqual(localDate))
        TaskState.Completed
      else if (taskStartDate.isBefore(localDate))
        TaskState.StartInPast
      else
        TaskState.EndInFuture
  }

}
