package com.eg.timeTrackingHelper.repository.jira.codec.html

import java.text.SimpleDateFormat
import java.util.{Locale, TimeZone}

import cats.implicits._
import com.eg.timeTrackingHelper.repository.jira.codec.exception.{HtmlDeserializationError, PathNotFoundException}
import com.eg.timeTrackingHelper.repository.jira.model.{DatePeriod, TicketStatus, TicketStatusStatistics, User}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

import scala.util.Try

private[jira] object Decoder {
  private val browser = JsoupBrowser()

  sealed trait Decoder[T] {
    def parse(content: String): Either[HtmlDeserializationError, T]
  }

  private[html] sealed trait TicketStatusStatisticsDecoder {
    private val defaultTimeZone = TimeZone.getTimeZone("UTC")
    private val simpleDateFormat = new SimpleDateFormat("dd/LLL/yy h:mm a", Locale.US)
    simpleDateFormat.setTimeZone(defaultTimeZone)
    private val regex: String = " - | : "

    private def getUser(
                         htmlElement: Element
                       ): Either[PathNotFoundException, User] =
      tryGetUser(htmlElement)
        .orElse(
          tryGetUserFromAlternativeSource(htmlElement)
        ).map(_.asRight)
        .getOrElse(
          PathNotFoundException(
            s"${CssSelector.user} | ${CssSelector.alternativeSourceUser}"
          ).asLeft
        )

    private def tryGetUserFromAlternativeSource(
                                                 htmlElement: Element
                                               ): Option[User] =
      (htmlElement >?> element(CssSelector.alternativeSourceUser))
        .map(element =>
          User(
            element.text,
            element.text
          )
        )

    private def tryGetUser(
                            htmlElement: Element
                          ): Option[User] =
      (htmlElement >?> element(CssSelector.user))
        .map(element =>
          User(
            element.attr("rel"),
            element.text
          ))

    private def parseStatuses(
                               htmlElement: Element
                             ): Either[Throwable, Map[TicketStatus, List[DatePeriod]]] =
      htmlElement >> elementList(CssSelector.statuses) match {
        case Nil => PathNotFoundException(CssSelector.statuses).asLeft
        case list => list.traverse { htmlElement =>
          for {
            ticketStatus <- getTicketStatus(htmlElement)
            dateTimeStrings <- getDateTimeStrings(htmlElement)
            datePeriods <- parseDateTimePeriod(dateTimeStrings)
          } yield (ticketStatus, datePeriods)
        }.map(_.toMap)
      }

    private def getTicketStatus(
                                 htmlElement: Element
                               ): Either[PathNotFoundException, TicketStatus] =
      (htmlElement >?> text(CssSelector.ticketStatus))
        .fold(
          PathNotFoundException(CssSelector.ticketStatus).asLeft[TicketStatus]
        )(
          TicketStatus(_).asRight[PathNotFoundException]
        )

    private def getDateTimeStrings(
                                    htmlElement: Element
                                  ): Either[PathNotFoundException, List[String]] =
      (htmlElement >> texts(CssSelector.dateTimeStrings)).toList match {
        case Nil => PathNotFoundException(CssSelector.dateTimeStrings).asLeft
        case list => list.asRight
      }

    private def parseDateTimePeriod(
                                     strings: List[String]
                                   ): Either[Throwable, List[DatePeriod]] =
      strings.mapFilter(
        _.split(regex) match {
          case Array(start, end, _) =>
            (start, end).some
          case _ => None
        }
      ).traverse(
        period =>
          Try(
            DatePeriod(
              simpleDateFormat.parse(period._1),
              simpleDateFormat.parse(period._2)
            )
          ).toEither
      )

    protected def parse(
                         htmlElement: Element
                       ): Either[Throwable, TicketStatusStatistics] =
      for {
        user <- getUser(htmlElement)
        statuses <- parseStatuses(htmlElement)
      } yield
        TicketStatusStatistics(
          user,
          statuses
        )

    private object CssSelector {
      val user = "td > a"
      val alternativeSourceUser = "td"
      val statuses = "td.normal-cell > table > tbody > tr"
      val ticketStatus = "td > span"
      val dateTimeStrings = "td > table > tbody > tr > td"
    }

  }

  implicit val listTicketStatusStatisticsDecoder =
    new Decoder[List[TicketStatusStatistics]] with TicketStatusStatisticsDecoder {
      private val mainSelector = ".issuePanelContainer > table > tbody > tr"

      override def parse(content: String): Either[HtmlDeserializationError, List[TicketStatusStatistics]] =
        Try(
          browser.parseString(content) >> elementList(mainSelector) match {
            case Nil => PathNotFoundException(mainSelector).asLeft
            case list => list.traverse(parse)
          }
        ).toEither
          .flatten
          .leftMap(
            cause => HtmlDeserializationError(cause)
          )
    }

  implicit val ticketStatusStatisticsDecoder: Decoder[TicketStatusStatistics] =
    new Decoder[TicketStatusStatistics] with TicketStatusStatisticsDecoder {

      override def parse(content: String): Either[HtmlDeserializationError, TicketStatusStatistics] =
        Try(
          parse(browser.parseString(content).body)
        ).toEither
          .flatten
          .leftMap(
            cause => HtmlDeserializationError(cause)
          )
    }


  implicit class DecoderOps(content: String) {
    def toEntity[T: Decoder]: Either[HtmlDeserializationError, T] =
      implicitly[Decoder[T]].parse(content)
  }

}
