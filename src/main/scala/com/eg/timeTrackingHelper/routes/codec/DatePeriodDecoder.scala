package com.eg.timeTrackingHelper.routes.codec

import java.time.LocalDate

import cats.effect.IO
import cats.syntax.either._
import com.eg.timeTrackingHelper.model.DatePeriod
import com.eg.timeTrackingHelper.utils.JsonCodecHelper
import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import org.http4s.circe.jsonOf

private[codec] trait DatePeriodDecoder extends JsonCodecHelper {
  implicit val datePeriodDecoder = new Decoder[DatePeriod] {
    override def apply(c: HCursor): Result[DatePeriod] =
      for {
        start <- parseLocalDate(c, "start")
        end <- parseLocalDate(c, "end")
      } yield DatePeriod(start, end)
  }

  implicit val resDatePeriodDecoder = jsonOf[IO, DatePeriod]

  private def parseLocalDate(
                              c: HCursor,
                              fieldName: String
                            ): Either[DecodingFailure, LocalDate] =
    for {
      jsonField <- c.downField(fieldName)
        .focus
        .fold(
          buildMandatoryFieldFailure(fieldName).asLeft[Json]
        )(_.asRight[DecodingFailure])
      strValue <- jsonField.asString
        .fold(
          DecodingFailure(s"The field '$fieldName' must be of string type.", List.empty).asLeft[String]
        )(_.asRight[DecodingFailure])
      localDate <- Either.catchNonFatal(LocalDate.parse(strValue)).leftMap(_ => DecodingFailure(
        s"The '$fieldName' field contains incorrect data. Valid data example - 2019-11-03.",
        List.empty
      ))
    } yield localDate
}