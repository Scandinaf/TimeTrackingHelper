package com.eg.timeTrackingHelper.utils

import cats.effect.IO
import cats.effect.concurrent.Ref
import com.eg.timeTrackingHelper.routes.exception.AccessTokenEmptyException

object AccessTokenHelper {
  def getAccessToken(tokenRef: Ref[IO, Option[String]]): IO[String] =
    tokenRef.get.flatMap {
      case Some(value) => IO.pure(value)
      case _           => IO.raiseError(AccessTokenEmptyException())
    }
}
