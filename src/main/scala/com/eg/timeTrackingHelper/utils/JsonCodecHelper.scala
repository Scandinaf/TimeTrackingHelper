package com.eg.timeTrackingHelper.utils

import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, Json}
import cats.syntax.either._

trait JsonCodecHelper {

  protected def getRequiredField[A: Decoder](
                                              json: Option[Json],
                                              fieldName: String
                                            ): Result[A] =
    json match {
      case Some(value) => value.as[A]
      case _ => buildMandatoryFieldFailure(fieldName).asLeft
    }

  protected def buildMandatoryFieldFailure(fieldName: String): DecodingFailure =
    DecodingFailure(s"The '$fieldName' field is mandatory.", List.empty)
}
