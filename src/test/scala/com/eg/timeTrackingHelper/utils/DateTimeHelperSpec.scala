package com.eg.timeTrackingHelper.utils

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class DateTimeHelperSpec extends AnyFlatSpec
  with Matchers
  with EitherValues {

  "DateTimeHelper.scaleToTimeFrame" should "return a correctly time frame #1" in {
    val timeFrame = 15.minutes.toSeconds.toInt
    val duration = 7.minutes.toSeconds.toInt
    val expected = 15.minutes.toSeconds.toInt
    DateTimeHelper.scaleToTimeFrame(duration, timeFrame) should be(expected)
  }

  it should "return a correctly time frame #2" in {
    val timeFrame = 15.minutes.toSeconds.toInt
    val duration = 35.minutes.toSeconds.toInt
    val expected = 45.minutes.toSeconds.toInt
    DateTimeHelper.scaleToTimeFrame(duration, timeFrame) should be(expected)
  }

  it should "return a correctly time frame #3" in {
    val timeFrame = 15.minutes.toSeconds.toInt
    val duration = 64.minutes.toSeconds.toInt
    val expected = 75.minutes.toSeconds.toInt
    DateTimeHelper.scaleToTimeFrame(duration, timeFrame) should be(expected)
  }

  it should "return a correctly time frame #4" in {
    val timeFrame = 15.minutes.toSeconds.toInt
    val duration = 0
    val expected = 15.minutes.toSeconds.toInt
    DateTimeHelper.scaleToTimeFrame(duration, timeFrame) should be(expected)
  }
}
