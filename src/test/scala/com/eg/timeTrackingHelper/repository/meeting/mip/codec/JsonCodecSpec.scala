package com.eg.timeTrackingHelper.repository.meeting.mip.codec

import java.time.LocalDateTime

import com.eg.timeTrackingHelper.repository.meeting.model.Meeting
import io.circe.parser._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.eg.timeTrackingHelper.repository.meeting.mip.codec.JsonCodec._

import scala.concurrent.duration._

class JsonCodecSpec extends AnyFlatSpec with Matchers with EitherValues {

  "JsonCodec" should "correctly parse json to meeting entities" in {
    val json = parse(loadResource("decoder/MIPMeetings.json")).value
    val meetings = json.as[List[Meeting]].value

    meetings.size shouldBe 4

    val meeting1 =
      meetings.find(_.subject == Option("Extension of employment agreement | Siarhei Barouski")).get
    meeting1.isTakePlace shouldBe true
    meeting1.duration shouldBe 30.minutes
    meeting1.start shouldBe LocalDateTime.parse("2020-05-25T12:30")
    meeting1.end shouldBe LocalDateTime.parse("2020-05-25T13:00")

    val meeting2 = meetings.find(_.subject == Option("Canceled: Int platform standup")).get
    meeting2.isTakePlace shouldBe false
    meeting2.duration shouldBe 30.minutes
    meeting2.start shouldBe LocalDateTime.parse("2020-05-25T10:30")
    meeting2.end shouldBe LocalDateTime.parse("2020-05-25T11:00")

    val meeting3 = meetings.find(_.subject == Option("One Wallet daily standup")).get
    meeting3.isTakePlace shouldBe true
    meeting3.duration shouldBe 30.minutes
    meeting3.start shouldBe LocalDateTime.parse("2020-05-25T10:30")
    meeting3.end shouldBe LocalDateTime.parse("2020-05-25T11:00")

    val meeting4 =
      meetings.find(_.subject == Option("INT planning/actualities/grooming; server side LOW")).get
    meeting4.isTakePlace shouldBe true
    meeting4.duration shouldBe 45.minutes
    meeting4.start shouldBe LocalDateTime.parse("2020-05-25T15:45")
    meeting4.end shouldBe LocalDateTime.parse("2020-05-25T16:30")
  }

  private def loadResource(filename: String): String =
    scala.io.Source.fromResource(filename).mkString
}
