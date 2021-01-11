package com.eg.timeTrackingHelper

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.eg.timeTrackingHelper.configuration.ApplicationConfig
import com.eg.timeTrackingHelper.handling.ConsoleExceptionHandling
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.repository.meeting.outlook.OutlookMeetingRepository
import com.eg.timeTrackingHelper.service.TimeTrackingService
import com.eg.timeTrackingHelper.service.meeting.MeetingService
import com.eg.timeTrackingHelper.service.validation.DatePeriodValidator
import com.eg.timeTrackingHelper.service.validation.exception.ValidationException
import com.eg.timeTrackingHelper.utils.ConsoleReader
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext

object Main extends IOApp with StrictLogging with ConsoleExceptionHandling {

  private val blocker = {
    val fixedThreadPool = Executors.newFixedThreadPool(2)
    val blockingIO = ExecutionContext.fromExecutor(fixedThreadPool)
    Blocker.liftExecutionContext(blockingIO)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      datePeriod <- contextShift.blockOn(blocker)(getDatePeriod)
      meetingRepository <- IO(OutlookMeetingRepository(ApplicationConfig.outlookConfig))
      meetingService <-
        IO(MeetingService(meetingRepository, ApplicationConfig.applicationSettings.keywordMapping))
      _ <- TimeTrackingService(meetingService).logTime(datePeriod)
    } yield ExitCode.Success)
      .handleErrorWith(throwable =>
        IO(logger.error("The application couldn't be completed correctly", throwable)) *> handle(
          throwable
        ).map(_ => ExitCode.Error)
      )
  }

  def getDatePeriod: IO[DatePeriod] =
    for {
      startPeriod <- ConsoleReader.getLocalDate("Start period(YYYY-MM-DD):")
      endPeriod <- ConsoleReader.getLocalDate("End period(YYYY-MM-DD):")
      datePeriod <- IO.fromEither(
        DatePeriodValidator
          .validate(DatePeriod(startPeriod, endPeriod))
          .leftMap(ValidationException)
          .toEither
      )
    } yield datePeriod
}
