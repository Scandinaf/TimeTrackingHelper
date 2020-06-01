package com.eg.timeTrackingHelper.service.ticket

import java.time.{LocalDate, LocalDateTime, Month}
import java.util.{Calendar, Date, TimeZone}

import cats.effect.IO
import cats.syntax.option._
import com.allantl.jira4s.v2.domain._
import com.allantl.jira4s.v2.domain.enums.SearchExpand
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.jira.JiraRepository
import com.eg.timeTrackingHelper.repository.jira.model.{TicketId, TicketStatus, TicketStatusStatistics, User, DatePeriod => DatePeriodJira}
import com.eg.timeTrackingHelper.service.model.{ActivityType, Assignee, WorklogEntity}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TicketServiceSpec extends AnyFlatSpec
  with MockitoSugar
  with Matchers
  with EitherValues {

  val jiraRepository = mock[JiraRepository]
  val currentUser = Assignee("testUser")
  val inProgressTicketStatus = TicketStatus("In Progress")
  val readyForReviewTicketStatus = TicketStatus("Ready for Review")
  val majorTicketStatuses: Set[TicketStatus] = Set(inProgressTicketStatus)
  val minorTicketStatuses: Set[TicketStatus] = Set(readyForReviewTicketStatus)
  val ticketService = TicketService(
    jiraRepository,
    currentUser,
    majorTicketStatuses,
    minorTicketStatuses
  )
  val ticketId = TicketId("INT-29410")

  "TicketService" should "return empty map" in {
    val start = LocalDate.of(2020, 3, 31)
    val end = start.plusDays(1)
    val datePeriod = DatePeriod(start, end)
    val searchResults = buildSearchResults(List(
      ("2020-01-14T11:27:57.623+0000", List(
        HistoryItem(
          field = "summary",
          fieldType = "jira",
          fieldId = None,
          from = None,
          fromString = "DS duplicate retry".some,
          to = None,
          tostring = "[Safe2] DS duplicate retry".some
        )
      ))
    ))
    when(jiraRepository.getMyTicketsByDatePeriod(
      ArgumentMatchers.eq(datePeriod),
      any[Set[TicketStatus]],
      any[Option[Set[String]]],
      any[Option[Set[SearchExpand]]],
      any[Int]
    )).thenReturn(IO.pure(searchResults))
    val result = ticketService.getTicketsLogEntity(datePeriod).unsafeRunSync()
    result shouldBe empty
  }

  it should "return one entity" in {
    val start = LocalDate.of(2020, 3, 31)
    val end = start.plusDays(1)
    val datePeriod = DatePeriod(start, end)
    val searchResults = buildSearchResults(List(
      ("2020-01-14T11:27:57.623+0000", List(
        HistoryItem(
          field = "summary",
          fieldType = "jira",
          fieldId = None,
          from = None,
          fromString = "DS duplicate retry".some,
          to = None,
          tostring = "[Safe2] DS duplicate retry".some
        )
      )),
      ("2020-01-14T11:28:15.254+0000", List(
        HistoryItem(
          field = "Sprint",
          fieldType = "custom",
          fieldId = None,
          from = "2232".some,
          fromString = "RB #69 (17.02.-28.02.)".some,
          to = "".some,
          tostring = "".some
        )
      )),
      ("2020-01-16T14:30:41.749+0000", List(
        HistoryItem(
          field = "assignee",
          fieldType = "jira",
          fieldId = None,
          from = "intbacklog".some,
          fromString = "INT Backlog".some,
          to = currentUser.userName.some,
          tostring = "Current User".some
        ),
        HistoryItem(
          field = "status",
          fieldType = "jira",
          fieldId = None,
          from = "10000".some,
          fromString = "Open".some,
          to = "3".some,
          tostring = inProgressTicketStatus.value.some
        )
      ))
    ))
    when(jiraRepository.getMyTicketsByDatePeriod(
      ArgumentMatchers.eq(datePeriod),
      any[Set[TicketStatus]],
      any[Option[Set[String]]],
      any[Option[Set[SearchExpand]]],
      any[Int]
    )).thenReturn(IO.pure(searchResults))
    val result = ticketService
      .getTicketsLogEntity(datePeriod)
      .unsafeRunSync()
      .get(LocalDate.of(2020, Month.JANUARY, 16))
    result.isDefined shouldBe true
    result.get.size shouldBe 1
    val workLogEntityResult = result.get.head
    workLogEntityResult.start should be(LocalDateTime.of(
      2020,
      Month.JANUARY,
      16,
      17,
      30,
      41,
      749000000
    ))
    workLogEntityResult.ticketId shouldBe ticketId
    workLogEntityResult.activityType shouldBe ActivityType.Major
  }

  it should "return one full entity" in {
    val start = LocalDate.of(2020, 3, 31)
    val end = start.plusDays(1)
    val datePeriod = DatePeriod(start, end)
    val searchResults = buildSearchResults(List(
      ("2020-01-14T11:27:57.623+0000", List(
        HistoryItem(
          field = "summary",
          fieldType = "jira",
          fieldId = None,
          from = None,
          fromString = "DS duplicate retry".some,
          to = None,
          tostring = "[Safe2] DS duplicate retry".some
        )
      )),
      ("2020-01-14T11:28:15.254+0000", List(
        HistoryItem(
          field = "Sprint",
          fieldType = "custom",
          fieldId = None,
          from = "2232".some,
          fromString = "RB #69 (17.02.-28.02.)".some,
          to = "".some,
          tostring = "".some
        )
      )),
      ("2020-01-16T14:30:41.749+0000", List(
        HistoryItem(
          field = "assignee",
          fieldType = "jira",
          fieldId = None,
          from = "intbacklog".some,
          fromString = "INT Backlog".some,
          to = currentUser.userName.some,
          tostring = "Current User".some
        ),
        HistoryItem(
          field = "status",
          fieldType = "jira",
          fieldId = None,
          from = "10000".some,
          fromString = "Open".some,
          to = "3".some,
          tostring = inProgressTicketStatus.value.some
        )
      )),
      ("2020-01-17T11:22:41.749+0000", List(
        HistoryItem(
          field = "description",
          fieldType = "jira",
          fieldId = None,
          from = None,
          fromString = "Some Text".some,
          to = None,
          tostring = "New Some Text".some
        )
      )),
      ("2020-01-17T11:40:32.749+0000", List(
        HistoryItem(
          field = "status",
          fieldType = "jira",
          fieldId = None,
          from = "3".some,
          fromString = inProgressTicketStatus.value.some,
          to = "745".some,
          tostring = "Close".some
        )
      ))
    ))
    when(jiraRepository.getMyTicketsByDatePeriod(
      ArgumentMatchers.eq(datePeriod),
      any[Set[TicketStatus]],
      any[Option[Set[String]]],
      any[Option[Set[SearchExpand]]],
      any[Int]
    )).thenReturn(IO.pure(searchResults))
    val result = ticketService.getTicketsLogEntity(datePeriod).unsafeRunSync()
    result.get(LocalDate.of(2020, Month.JANUARY, 16)) should be(List(
      WorklogEntity(
        LocalDateTime.of(
          2020,
          Month.JANUARY,
          16,
          17,
          30,
          41,
          749000000
        ),
        end = LocalDateTime.of(
          2020,
          Month.JANUARY,
          17,
          14,
          40,
          32,
          749000000
        ),
        ticketId,
        ActivityType.Major,
      )
    ).some)
  }

  it should "return one full entity based on alternative way" in {
    val start = LocalDate.of(2020, 3, 31)
    val end = start.plusDays(1)
    val datePeriod = DatePeriod(start, end)
    val searchResults = buildSearchResults(List(
      ("2asdsasdasd020-01-16T14:30:41.749+0000", List(
        HistoryItem(
          field = "assignee",
          fieldType = "jira",
          fieldId = None,
          from = "intbacklog".some,
          fromString = "INT Backlog".some,
          to = currentUser.userName.some,
          tostring = "Current User".some
        ),
        HistoryItem(
          field = "status",
          fieldType = "jira",
          fieldId = None,
          from = "10000".some,
          fromString = "Open".some,
          to = "3".some,
          tostring = inProgressTicketStatus.value.some
        )
      )),
    ))
    when(jiraRepository.getMyTicketsByDatePeriod(
      ArgumentMatchers.eq(datePeriod),
      any[Set[TicketStatus]],
      any[Option[Set[String]]],
      any[Option[Set[SearchExpand]]],
      any[Int]
    )).thenReturn(IO.pure(searchResults))
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.set(2020, 0, 14, 16, 30, 22)
    calendar.set(Calendar.MILLISECOND, 560)
    val dateStart = Date.from(calendar.toInstant)
    calendar.set(2020, 0, 15, 16, 30, 22)
    calendar.set(Calendar.MILLISECOND, 560)
    val dateEnd = Date.from(calendar.toInstant)
    val ticketStatusStatistics = List(
      TicketStatusStatistics(
        User(
          "sbarouski",
          "Siarhei Barouski"
        ),
        Map(
          (inProgressTicketStatus, List(
            DatePeriodJira(
              dateStart,
              dateEnd
            ))),
          (TicketStatus("Open"), List(
            DatePeriodJira(
              dateStart,
              dateEnd
            ))),
          (TicketStatus("Ready For Cit1"), List(
            DatePeriodJira(
              dateStart,
              dateEnd
            )))
        )
      ),
      TicketStatusStatistics(
        User(
          currentUser.userName,
          "Current User"
        ),
        Map(
          (inProgressTicketStatus, List(
            DatePeriodJira(
              dateStart,
              dateEnd
            ))),
          (TicketStatus("Open"), List(
            DatePeriodJira(
              dateStart,
              dateEnd
            ))),
          (readyForReviewTicketStatus, List(
            DatePeriodJira(
              dateStart,
              dateEnd
            ))),
          (TicketStatus("Ready for Cit1"), List(
            DatePeriodJira(
              dateStart,
              dateEnd
            )))
        )
      )
    )
    when(jiraRepository.getTicketStatusStatistics(ticketId))
      .thenReturn(IO.pure(ticketStatusStatistics))
    val result = ticketService.getTicketsLogEntity(datePeriod).unsafeRunSync()
    verify(jiraRepository, times(1)).getTicketStatusStatistics(ticketId)
    result.size should be(2)
    result.get(LocalDate.of(2020, Month.JANUARY, 14)) should be(List(
      WorklogEntity(
        LocalDateTime.of(
          2020,
          Month.JANUARY,
          14,
          19,
          30,
          22,
          560000000
        ),
        end = LocalDateTime.of(
          2020,
          Month.JANUARY,
          15,
          19,
          30,
          22,
          560000000
        ),
        ticketId,
        ActivityType.Major,
      ),
      WorklogEntity(
        LocalDateTime.of(
          2020,
          Month.JANUARY,
          14,
          19,
          30,
          22,
          560000000
        ),
        end = LocalDateTime.of(
          2020,
          Month.JANUARY,
          15,
          19,
          30,
          22,
          560000000
        ),
        ticketId,
        ActivityType.Minor,
      )
    ).some)
  }

  it should "сorrectly process scenario #1" in {
    val start = LocalDate.of(2020, 3, 31)
    val end = start.plusDays(1)
    val datePeriod = DatePeriod(start, end)
    val searchResults = SearchResults(
      "renderedFields,names,schema,operations,editmeta,changelog,versionedRepresentations".some,
      0,
      200,
      10,
      List(
        Issue(
          "1232469",
          "https://jira.evolutiongaming.com/rest/api/2/issue/1232469",
          ticketId.value,
          changelog = Some(Changelog(
            0,
            200,
            10,
            List(
              History(
                "1",
                null,
                "2020-01-16T14:30:41.749+0000",
                List(
                  HistoryItem(
                    field = "description",
                    fieldType = "jira",
                    fieldId = None,
                    from = "intbacklog".some,
                    fromString = "INT Backlog".some,
                    to = currentUser.userName.some,
                    tostring = "Current User".some
                  ),
                )
              ),
              History(
                "2",
                null,
                "2020-01-16T14:35:41.749+0000",
                List(
                  HistoryItem(
                    field = "assignee",
                    fieldType = "jira",
                    fieldId = None,
                    from = "intbacklog".some,
                    fromString = "INT Backlog".some,
                    to = currentUser.userName.some,
                    tostring = "Current User".some
                  ),
                )
              ),
              History(
                "3",
                null,
                "2020-01-17T10:35:41.749+0000",
                List(
                  HistoryItem(
                    field = "assignee",
                    fieldType = "jira",
                    fieldId = None,
                    from = currentUser.userName.some,
                    fromString = "Current User".some,
                    to = "sbarouski".some,
                    tostring = "Sirahei Barouski".some
                  ),
                  HistoryItem(
                    field = "status",
                    fieldType = "jira",
                    fieldId = None,
                    from = "10000".some,
                    fromString = "Open".some,
                    to = "3".some,
                    tostring = inProgressTicketStatus.value.some
                  ),
                )
              ),
              History(
                "4",
                null,
                "2020-01-18T10:35:41.749+0000",
                List(
                  HistoryItem(
                    field = "assignee",
                    fieldType = "jira",
                    fieldId = None,
                    from = "sbarouski".some,
                    fromString = "Sirahei Barouski".some,
                    to = currentUser.userName.some,
                    tostring = "Current User".some,
                  ),
                )
              ),
              History(
                "5",
                null,
                "2020-01-18T11:45:41.749+0000",
                List(
                  HistoryItem(
                    field = "status",
                    fieldType = "jira",
                    fieldId = None,
                    from = "3".some,
                    fromString = inProgressTicketStatus.value.some,
                    to = "123".some,
                    tostring = "Close".some,
                  ),
                )
              )
            )
          ))
        ),
        Issue(
          "1232470",
          "https://jira.evolutiongaming.com/rest/api/2/issue/1232470",
          "INT-23444",
          changelog = Some(Changelog(
            0,
            200,
            10,
            List(
              History(
                "1",
                null,
                "2020-01-23T14:30:41.749+0000",
                List(
                  HistoryItem(
                    field = "description",
                    fieldType = "jira",
                    fieldId = None,
                    from = "intbacklog".some,
                    fromString = "INT Backlog".some,
                    to = currentUser.userName.some,
                    tostring = "Current User".some
                  ),
                )
              ),
              History(
                "2",
                null,
                "2020-01-23T14:35:41.749+0000",
                List(
                  HistoryItem(
                    field = "assignee",
                    fieldType = "jira",
                    fieldId = None,
                    from = "intbacklog".some,
                    fromString = "INT Backlog".some,
                    to = currentUser.userName.some,
                    tostring = "Current User".some
                  ),
                )
              ),
              History(
                "3",
                null,
                "2020-01-24T10:35:41.749+0000",
                List(
                  HistoryItem(
                    field = "assignee",
                    fieldType = "jira",
                    fieldId = None,
                    from = currentUser.userName.some,
                    fromString = "Current User".some,
                    to = "sbarouski".some,
                    tostring = "Sirahei Barouski".some
                  ),
                  HistoryItem(
                    field = "status",
                    fieldType = "jira",
                    fieldId = None,
                    from = "10000".some,
                    fromString = "Open".some,
                    to = "3".some,
                    tostring = inProgressTicketStatus.value.some
                  ),
                )
              ),
              History(
                "4",
                null,
                "2020-01-25T10:35:41.749+0000",
                List(
                  HistoryItem(
                    field = "assignee",
                    fieldType = "jira",
                    fieldId = None,
                    from = "sbarouski".some,
                    fromString = "Sirahei Barouski".some,
                    to = currentUser.userName.some,
                    tostring = "Current User".some,
                  ),
                )
              ),
              History(
                "5",
                null,
                "2020-01-25T11:45:41.749+0000",
                List(
                  HistoryItem(
                    field = "status",
                    fieldType = "jira",
                    fieldId = None,
                    from = "3".some,
                    fromString = inProgressTicketStatus.value.some,
                    to = "123".some,
                    tostring = "Close".some,
                  ),
                )
              )
            )
          ))
        )
      )
    )
    when(jiraRepository.getMyTicketsByDatePeriod(
      ArgumentMatchers.eq(datePeriod),
      any[Set[TicketStatus]],
      any[Option[Set[String]]],
      any[Option[Set[SearchExpand]]],
      any[Int]
    )).thenReturn(IO.pure(searchResults))
    val result = ticketService.getTicketsLogEntity(datePeriod).unsafeRunSync()
    result.size should be(2)
    result.get(LocalDate.of(2020, Month.JANUARY, 25)) should be(List(
      WorklogEntity(
        LocalDateTime.of(2020, Month.JANUARY, 25, 13, 35, 41, 749000000),
        LocalDateTime.of(2020, Month.JANUARY, 25, 14, 45, 41, 749000000),
        TicketId("INT-23444"),
        ActivityType.Major
      )
    ).some)
  }

  private def buildSearchResults(histories: List[(String, List[HistoryItem])]): SearchResults =
    SearchResults(
      "renderedFields,names,schema,operations,editmeta,changelog,versionedRepresentations".some,
      0,
      200,
      10,
      List(
        Issue(
          "1232469",
          "https://jira.evolutiongaming.com/rest/api/2/issue/1232469",
          "INT-29410",
          changelog = Some(Changelog(
            0,
            200,
            10,
            histories.map(item =>
              History(
                "1",
                null,
                item._1,
                item._2
              )
            )
          ))
        )
      )
    )
}
