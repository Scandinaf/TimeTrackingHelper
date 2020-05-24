package com.eg.timeTrackingHelper.repository.meeting.outlook

import java.net.URI

import cats.effect.{Async, IO}
import cats.implicits._
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.meeting.outlook.exception.{ServerNotFoundException, UnauthorizedException, UnknownException}
import com.eg.timeTrackingHelper.utils.DateTimeHelper.LocalDateToDate
import microsoft.exchange.webservices.data.core.ExchangeService
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName
import microsoft.exchange.webservices.data.core.exception.service.remote.ServiceRequestException
import microsoft.exchange.webservices.data.core.service.folder.CalendarFolder
import microsoft.exchange.webservices.data.core.service.item.Appointment
import microsoft.exchange.webservices.data.credential.WebCredentials
import microsoft.exchange.webservices.data.search.CalendarView

import scala.collection.JavaConverters._

private[outlook] trait OutlookExchangeService {
  private val maxItemsReturned = 200
  private val exchangeService = (buildExchangeService _).tupled(getSettings)
  private lazy val calendarFolderIO = buildCalendarFolder

  protected def getSettings: (String, String, URI)

  protected def findOutlookAppointments(datePeriod: DatePeriod): IO[List[Appointment]] =
    for {
      calendarFolder <- calendarFolderIO
      result <- findAppointments(calendarFolder, datePeriod)
    } yield result

  private def buildExchangeService(email: String, password: String, url: URI): ExchangeService = {
    val service: ExchangeService = new ExchangeService(ExchangeVersion.Exchange2010_SP2)
    service.setCredentials(new WebCredentials(email, password))
    service.setUrl(url)
    service
  }

  private def buildCalendarFolder: IO[CalendarFolder] =
    Async[IO].async(
      _ (
        Either.catchNonFatal {
          CalendarFolder.bind(exchangeService, WellKnownFolderName.Calendar)
        }.leftMap({
          case x: ServiceRequestException if x.getMessage.contains("(401)Unauthorized") =>
            UnauthorizedException(x)
          case x: ServiceRequestException if x.getMessage.contains("(404)Not Found") =>
            ServerNotFoundException(x)
          case throwable: Throwable =>
            UnknownException(throwable)
        })
      )
    )

  private def findAppointments(
                                calendarFolder: CalendarFolder,
                                datePeriod: DatePeriod
                              ): IO[List[Appointment]] =
    Async[IO].async(
      _ (
        Either.catchNonFatal {
          calendarFolder
            .findAppointments(buildCalendarView(datePeriod))
        }.bimap(
          UnknownException(_),
          _.getItems.asScala.toList)
      )
    )

  private def buildCalendarView(datePeriod: DatePeriod): CalendarView =
    new CalendarView(
      datePeriod.start.toDate,
      datePeriod.end.toDate,
      maxItemsReturned
    )
}
