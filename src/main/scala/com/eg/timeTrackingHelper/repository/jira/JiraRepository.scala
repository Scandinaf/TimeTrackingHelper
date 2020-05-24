package com.eg.timeTrackingHelper.repository.jira

import java.time.format.DateTimeFormatter

import cats.effect.{Async, ContextShift, IO}
import com.allantl.jira4s.auth.BasicAuthentication
import com.allantl.jira4s.v2.JiraSingleTenantClient
import com.allantl.jira4s.v2.domain._
import com.allantl.jira4s.v2.domain.enums.SearchExpand
import com.eg.timeTrackingHelper.configuration.model.JiraConfig
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.jira.codec.html.Decoder.DecoderOps
import com.eg.timeTrackingHelper.repository.jira.model.{TicketId, TicketStatus, TicketStatusStatistics}
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id}

class JiraRepository(
                      config: JiraConfig
                    )(
                      implicit contextShift: ContextShift[IO]
                    ) extends Repository {
  private implicit val sttpBackend = HttpURLConnectionBackend()
  private val queryDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  private val client: JiraSingleTenantClient[Id] = JiraSingleTenantClient(
    BasicAuthentication(
      config.url.toString,
      config.userName,
      config.password
    )
  )

  def getTicketStatusStatistics(
                                 ticketId: TicketId
                               ): IO[List[TicketStatusStatistics]] =
    for {
      content <- Async[IO].async[String] { io =>
        io(
          client.getResourceHtml(
            path = s"/browse/${ticketId.value}",
            params = Map("page" -> "ru.andreymarkelov.atlas.plugins.datacollector:user-status-panel"),
            headers = Map("X-PJAX" -> "true"),
          )
        )
      }
      result <- IO.fromEither(content.toEntity[List[TicketStatusStatistics]])
    } yield result


  def getTickets(
                  ticketIds: Set[TicketId],
                  fields: Option[Set[String]] = None
                ): IO[SearchResults] =
    Async[IO].async(_ (
      client.search(
        jql = s"id in (${ticketIds.map(_.value).mkString(",")})",
        fields = fields
      )
    ))

  def getTicket(
                 ticketId: TicketId,
                 fields: List[String] = List("*all")
               ): IO[Issue] =
    Async[IO].async(_ (client.getIssue(ticketId.value, fields)))

  def getMyTicketsByDatePeriod(
                                datePeriod: DatePeriod,
                                statuses: Set[TicketStatus],
                                fields: Option[Set[String]] = None,
                                expand: Option[Set[SearchExpand]] = None,
                                maxResults: Int = 200
                              ): IO[SearchResults] =
    Async[IO].async { io =>
      val periodParameters = dateToPeriodQueryParameters(datePeriod)
      io(
        client.search(
          maxResults = maxResults,
          jql =
            s"""assignee was currentUser()
               | $periodParameters
               | AND
               | status was in (${statuses.map(status => s""""${status.value}"""").mkString(",")})
               | $periodParameters""".stripMargin,
          fields = fields,
          expand = expand,
        )
      )
    }

  def addWorkLog(
                  ticketId: TicketId,
                  workLogPayLoad: WorkLogPayLoad,
                  adjustEstimate: AdjustEstimate
                ): IO[WorkLogCreateResponse] =
    Async[IO].async(_ (client.createWorkLog(ticketId.value, workLogPayLoad, adjustEstimate)))

  private def dateToPeriodQueryParameters(
                                           datePeriod: DatePeriod
                                         ): String = {
    s"""AFTER "${datePeriod.start.atStartOfDay().format(queryDateTimeFormatter)}"
       | BEFORE "${datePeriod.end.atStartOfDay().format(queryDateTimeFormatter)}"""".stripMargin
  }
}

object JiraRepository {
  def apply(
             config: JiraConfig
           )(
             implicit contextShift: ContextShift[IO]
           ): JiraRepository =
    new JiraRepository(config)(contextShift)
}