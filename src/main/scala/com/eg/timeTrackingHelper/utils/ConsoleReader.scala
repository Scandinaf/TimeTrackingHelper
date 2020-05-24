package com.eg.timeTrackingHelper.utils

import java.time.LocalDate

import cats.effect.IO

object ConsoleReader {

  def getLocalDate(msg: String, maxRetries: Int = 3): IO[LocalDate] =
    (for {
      _ <- IO(println(msg))
      dateAsString <- IO(scala.io.StdIn.readLine())
    } yield LocalDate.parse(dateAsString))
      .handleErrorWith(error =>
        if (maxRetries > 0)
          getLocalDate(msg, maxRetries - 1)
        else
          IO.raiseError(error)
      )
}
