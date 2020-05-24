package com.eg.timeTrackingHelper.service

import java.time.LocalDateTime

import com.eg.timeTrackingHelper.service.model.{ActivityType, HierarchicalWorklog, WorklogEntity}

private[service] trait HierarchicalWorklogBuilder {

  protected def toHierarchicalWorklogs(
                                        worklogs: List[WorklogEntity]
                                      ): HierarchicalWorklog = {
    val (major, minor) = worklogs.partition(_.activityType == ActivityType.Major)
    HierarchicalWorklog(
      minor,
      buildHierarchicalWorklogs(major)
    )
  }

  private def buildHierarchicalWorklogs(
                                         worklogs: List[WorklogEntity]
                                       ): List[WorklogEntity] =
    worklogs
      .sortWith((l, r) => l.start.isBefore(r.start))
      .foldLeft(
        List[WorklogEntity]()
      )(
        (list, worklogEntity) =>
          list.span(!splitCondition(_, worklogEntity)) match {
            case (l, Nil) => l :+ worklogEntity
            case (l, r) => l ::: makeSplit(worklogEntity, r.head, r.tail)
          }
      )

  private def makeSplit(
                         worklogEntity: WorklogEntity,
                         head: WorklogEntity,
                         tail: List[WorklogEntity]
                       ): List[WorklogEntity] =
    List(
      head.copy(
        end = worklogEntity.start
      ),
      worklogEntity,
    ) :::
      calculateTail(
        worklogEntity,
        worklogEntity.end,
        head,
        tail
      )

  private def calculateTail(
                             worklogEntity: WorklogEntity,
                             worklogEntityEnd: LocalDateTime,
                             head: WorklogEntity,
                             tail: List[WorklogEntity],
                           ): List[WorklogEntity] =
    if (isWorklogOverflow(head.end, worklogEntityEnd))
      if (isWorklogOverflow(tail, worklogEntityEnd))
        List.empty
      else
        List(
          tail.head.copy(
            start = worklogEntityEnd
          )
        )
    else
      head.copy(
        start = worklogEntityEnd
      ) :: tail

  /*
     Example:
     ticket1 [start: 11:00, end: 14:00]
     ticket2 [start: 12:00, end: 18:00]

    Calculates situations where one task starts within another but ends later.
   */
  private def isWorklogOverflow(
                                 headEnd: LocalDateTime,
                                 worklogEntityEnd: LocalDateTime
                               ): Boolean =
    headEnd.isBefore(worklogEntityEnd)

  /*
     Example:
     ticket1 [start: 11:00, end: 18:00]
     ticket2 [start: 12:00, end: 17:00]
     ticket3 [start: 13:00, end: 20:00]

     Calculates situations where one task starts within another but ends later.
   */
  private def isWorklogOverflow(
                                 tail: List[WorklogEntity],
                                 worklogEntityEnd: LocalDateTime
                               ): Boolean =
    tail.isEmpty || isWorklogOverflow(
      tail.head.end,
      worklogEntityEnd
    )

  private def splitCondition(
                              finalWorklogEntity: WorklogEntity,
                              worklogEntity: WorklogEntity
                            ): Boolean =
    worklogEntity
      .start
      .isBefore(
        finalWorklogEntity.end
      )
}
