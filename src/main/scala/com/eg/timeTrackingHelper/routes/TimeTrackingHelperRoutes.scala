package com.eg.timeTrackingHelper.routes

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, ContextShift, IO}
import cats.syntax.option._
import com.eg.timeTrackingHelper.configuration.ApplicationConfig
import com.eg.timeTrackingHelper.handling.RestExceptionHandling
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.routes.codec.JsonCodec._
import com.eg.timeTrackingHelper.service.TimeTrackingService
import com.eg.timeTrackingHelper.service.validation.DatePeriodValidator
import com.eg.timeTrackingHelper.service.validation.exception.ValidationException
import com.eg.timeTrackingHelper.utils.AccessTokenHelper
import com.typesafe.scalalogging.StrictLogging
import org.http4s.Uri.RegName
import org.http4s._
import org.http4s.dsl.io.{->, /, GET, Ok, POST, PermanentRedirect, Root}
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.server.middleware.AutoSlash

import scala.util.Random

class TimeTrackingHelperRoutes(
                                tokenRef: Ref[IO, Option[String]],
                                blocker: Blocker,
                                timeTrackingService: TimeTrackingService,
                              )(
                                implicit context: ContextShift[IO],
                              ) extends StrictLogging with RestExceptionHandling {

  private val oauth2Settings = ApplicationConfig.oauth2Settings
  private val nonce = Random.nextInt(Int.MaxValue)

  val routes: HttpRoutes[IO] = AutoSlash(HttpRoutes.of[IO] {
    case GET -> Root / "timeTrackingHelper" / "token" =>
      logger.info("A token generation request has been received.")
      PermanentRedirect(Location(buildTokenUri))

    case req@GET -> Root / "timeTrackingHelper" / "form" =>
      StaticFile.fromResource(
        "html/TimeTrackingForm.html",
        blocker,
        Some(req)
      ).getOrElseF(NotFound())

    case req@POST -> Root / "timeTrackingHelper" / "form" =>
      (for {
        _ <- IO(logger.info("Request to send the time tracking data has been received."))
        _ <- AccessTokenHelper.getAccessToken(tokenRef)
        datePeriod <- req.as[DatePeriod]
        _ <- IO.fromEither(DatePeriodValidator
          .validate(datePeriod)
          .leftMap(ValidationException)
          .toEither)
        _ <- timeTrackingService.logTime(datePeriod)
        response <- Ok(
          s"The process was successfully completed. Start - ${datePeriod.start}, End - ${datePeriod.end}."
        )
      } yield response).handleErrorWith(throwable =>
        IO(
          logger.error(
            "The request couldn't be completed correctly",
            throwable
          )
        ) *> handle(throwable)
      )

    case req@POST -> Root / "timeTrackingHelper" =>
      (for {
        _ <- IO(logger.info(s"Request to install a new token has been received."))
        response <- decodeUrlForm(req)
      } yield response).handleErrorWith(throwable =>
        IO(logger.error(
          "The request couldn't be completed correctly",
          throwable
        )) *> handle(throwable)
      )
  })

  private def decodeUrlForm(req: Request[IO]): IO[Response[IO]] =
    req.decode[UrlForm] { form =>
      getAccessToken(form).flatMap {
        case None => BadRequest("The access token cannot be empty.")
        case accessToken => tokenRef.set(accessToken) *> MovedPermanently(Location(uri"/timeTrackingHelper/form"))
      }
    }

  private def getAccessToken(urlForm: UrlForm): IO[Option[String]] =
    IO(
      urlForm.get("access_token").toList.filterNot(_.isBlank) match {
        case Nil => None
        case list => list.mkString.some
      }
    )

  private def buildTokenUri: Uri =
    Uri(
      scheme = Uri.Scheme.https.some,
      authority = Uri.Authority(host = RegName("login.microsoftonline.com")).some,
      path = s"${oauth2Settings.tenant}/oauth2/v2.0/authorize",
      query = Query.fromPairs(
        "client_id" -> oauth2Settings.clientId,
        "response_type" -> "token",
        "redirect_uri" -> oauth2Settings.redirectUrl.toString,
        "scope" -> "openid Calendars.Read.Shared",
        "response_mode" -> "form_post",
        "state" -> Random.nextInt(Int.MaxValue).toString,
        "nonce" -> nonce.toString,
      )
    )
}

object TimeTrackingHelperRoutes {
  def apply(
             tokenRef: Ref[IO, Option[String]],
             blocker: Blocker,
             timeTrackingService: TimeTrackingService,
           )(
             implicit context: ContextShift[IO],
           ): TimeTrackingHelperRoutes =
    new TimeTrackingHelperRoutes(tokenRef, blocker, timeTrackingService)
}