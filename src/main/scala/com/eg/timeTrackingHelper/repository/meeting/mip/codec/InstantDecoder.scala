package com.eg.timeTrackingHelper.repository.meeting.mip.codec

import java.time.Instant

import io.circe.Decoder.Result
import io.circe.{Decoder, HCursor}

private[codec] trait InstantDecoder {

  implicit val instantDecoder = new Decoder[Instant] {
    override def apply(c: HCursor): Result[Instant] =
      c.as[String].map(v => Instant.parse(s"${v.substring(0, 22)}Z"))
  }
}
