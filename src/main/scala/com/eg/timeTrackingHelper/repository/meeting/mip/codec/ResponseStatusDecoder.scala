package com.eg.timeTrackingHelper.repository.meeting.mip.codec

import cats.syntax.either._
import io.circe.{Decoder, DecodingFailure, HCursor}

private[codec] trait ResponseStatusDecoder {

  sealed trait ResponseStatus

  case object None extends ResponseStatus

  case object Organizer extends ResponseStatus

  case object TentativelyAccepted extends ResponseStatus

  case object Accepted extends ResponseStatus

  case object Declined extends ResponseStatus

  case object NotResponded extends ResponseStatus

  implicit val responseStatusDecoder: Decoder[ResponseStatus] = (c: HCursor) => {
    c.as[String].map(_.toLowerCase.trim).flatMap {
      case "none" => None.asRight
      case "organizer" => Organizer.asRight
      case "tentativelyaccepted" => TentativelyAccepted.asRight
      case "accepted" => Accepted.asRight
      case "declined" => Declined.asRight
      case "notresponded" => NotResponded.asRight
      case value => DecodingFailure(s"Nothing is known about the next type - $value.", List.empty).asLeft
    }
  }
}
