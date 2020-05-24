package com.eg.timeTrackingHelper

import java.awt.Desktop
import java.net.URI
import java.util.concurrent.Executors

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.eg.timeTrackingHelper.configuration.ApplicationConfig
import com.eg.timeTrackingHelper.configuration.model.ServerSettings
import com.eg.timeTrackingHelper.repository.meeting.mip.OutlookMeetingRepository
import com.eg.timeTrackingHelper.routes.TimeTrackingHelperRoutes
import com.eg.timeTrackingHelper.service.TimeTrackingService
import com.eg.timeTrackingHelper.service.meeting.MeetingService
import com.typesafe.scalalogging.StrictLogging
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

object Main_v2 extends IOApp with StrictLogging {

  private val resourceBlocker = {
    val fixedThreadPool = Executors.newFixedThreadPool(2)
    val blockingIO = ExecutionContext.fromExecutor(fixedThreadPool)
    Blocker.liftExecutionContext(blockingIO)
  }

  private val http4sBlocker = {
    val fixedThreadPool = Executors.newFixedThreadPool(5)
    val blockingIO = ExecutionContext.fromExecutor(fixedThreadPool)
    Blocker.liftExecutionContext(blockingIO)
  }

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      tokenRef <- Ref.of[IO, Option[String]](None)
      meetingRepository <- IO(OutlookMeetingRepository(tokenRef, http4sBlocker))
      meetingService <- IO(MeetingService(
        meetingRepository,
        ApplicationConfig.applicationSettings.keywordMapping
      ))
      timeTrackingService <- IO(TimeTrackingService(meetingService))
      serverSettings <- IO(ApplicationConfig.serverSettings)
      timeTrackingHelperRoutes <- IO(TimeTrackingHelperRoutes(tokenRef, resourceBlocker, timeTrackingService))
      webServerFiber <- BlazeServerBuilder[IO](global)
        .bindHttp(serverSettings.port, serverSettings.host)
        .withHttpApp(timeTrackingHelperRoutes.routes.orNotFound)
        .resource
        .use(_ => IO.never)
        .start
      _ <- IO.sleep(5.second) *> openBrowser(serverSettings)
      _ <- webServerFiber.join
    } yield ExitCode.Success).handleErrorWith(throwable =>
      IO(logger.error(
        "The application couldn't be completed correctly",
        throwable
      )) *> IO.pure(ExitCode.Error)
    )

  private def openBrowser(serverSettings: ServerSettings): IO[Unit] =
    IO(
      if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE))
        Desktop.getDesktop.browse(
          new URI(s"http://${serverSettings.host}:${serverSettings.port}/timeTrackingHelper/token")
        )
    )
}