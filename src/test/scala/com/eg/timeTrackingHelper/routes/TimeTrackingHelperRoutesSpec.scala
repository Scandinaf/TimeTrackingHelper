package com.eg.timeTrackingHelper.routes

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, IO}
import cats.syntax.option._
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.service.TimeTrackingService
import org.http4s._
import org.http4s.headers.Location
import org.http4s.implicits._
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class TimeTrackingHelperRoutesSpec
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterAll {

  private val accessTokenRef = Ref.of[IO, Option[String]](None)
  private val accessToken = "asdsadadsasd123123asddas12312asdasd"
  private val timeTrackingService = mock[TimeTrackingService]
  private val blocker = Blocker.liftExecutionContext(global)
  private implicit val contextShift = IO.contextShift(global)

  override protected def beforeAll(): Unit = {
    when(timeTrackingService.logTime(any[DatePeriod])).thenReturn(IO.unit)
    ()
  }

  "TimeTrackingHelperRoutes.installToken" should "process empty token correctly" in {
    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper")
        .withEntity(UrlForm("access_token" -> "")),
      Status.BadRequest,
      "The access token cannot be empty.".some
    )

    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper")
        .withEntity(UrlForm("not_access_token" -> accessToken)),
      Status.BadRequest,
      "The access token cannot be empty.".some
    )

    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper").withEntity("{'test':'test'}"),
      Status.BadRequest,
      "The request body was malformed.".some
    )
  }

  it should "moved permanently to '/timeTrackingHelper/form'" in {
    val result = check[Unit](
      Request(method = Method.POST, uri = uri"/timeTrackingHelper")
        .withEntity(UrlForm("access_token" -> accessToken)),
      Status.MovedPermanently
    )

    result._1.headers.find(_.is(Location)) shouldBe Location(uri"/timeTrackingHelper/form").some
  }

  "TimeTrackingHelperRoutes.PostTimeTrackingForm" should "process incorrect body correctly" in {
    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper/form")
        .withEntity(UrlForm("not_access_token" -> accessToken)),
      Status.BadRequest,
      """There is no point in continuing because the token hasn't been installed.
        |Please visit the next page 'GET /timeTrackingHelper/token'.""".stripMargin.some
    )

    val correctAccessToken = Ref.of[IO, Option[String]](accessToken.some).some
    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper/form")
        .withEntity(UrlForm("not_access_token" -> accessToken)),
      Status.BadRequest,
      "Invalid JSON".some,
      correctAccessToken
    )

    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper/form")
        .withEntity("{'asd' : 'asd'}"),
      Status.BadRequest,
      "Invalid JSON".some,
      correctAccessToken
    )

    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper/form")
        .withEntity("""{"asd" : "asd"}"""),
      Status.BadRequest,
      "The 'start' field is mandatory.".some,
      correctAccessToken
    )

    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper/form")
        .withEntity("""{"start" : "2020-05-21"}"""),
      Status.BadRequest,
      "The 'end' field is mandatory.".some,
      correctAccessToken
    )

    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper/form")
        .withEntity("""{"start" : "2020-05-21", "end" : "123"}"""),
      Status.BadRequest,
      "The 'end' field contains incorrect data. Valid data example - 2019-11-03.".some,
      correctAccessToken
    )

    check(
      Request(method = Method.POST, uri = uri"/timeTrackingHelper/form")
        .withEntity("""{"start" : "2020-05-21", "end" : "2020-05-14"}"""),
      Status.BadRequest,
      """During the validation process, the following errors were detected:
        |The start cannot be less than the end!!!
        |""".stripMargin.some,
      correctAccessToken
    )
  }

  private def check[A](
    request: Request[IO],
    expectedStatus: Status,
    expectedBody: Option[A] = None,
    accessTokenOpt: Option[IO[Ref[IO, Option[String]]]] = None
  )(implicit decoder: EntityDecoder[IO, A]): (Response[IO], A) = {
    val result = (for {
      accessToken <- accessTokenOpt.getOrElse(accessTokenRef)
      routes <- IO(TimeTrackingHelperRoutes(accessToken, blocker, timeTrackingService).routes)
      response <- routes.orNotFound.run(request)
      body <- response.as[A]
    } yield (response, body)).unsafeRunSync()

    result._1.status shouldBe expectedStatus
    expectedBody.foreach(result._2 shouldBe _)
    result
  }
}
