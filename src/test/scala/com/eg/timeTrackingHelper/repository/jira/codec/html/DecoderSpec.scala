package com.eg.timeTrackingHelper.repository.jira.codec.html

import java.text.{ParseException, SimpleDateFormat}
import java.util.Locale

import com.eg.timeTrackingHelper.repository.jira.codec.exception.{HtmlDeserializationError, PathNotFoundException}
import com.eg.timeTrackingHelper.repository.jira.codec.html.Decoder.DecoderOps
import com.eg.timeTrackingHelper.repository.jira.model.{DatePeriod, TicketStatus, TicketStatusStatistics, User}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DecoderSpec extends AnyFlatSpec
  with Matchers
  with EitherValues {

  private val simpleDateFormat = new SimpleDateFormat("dd/LLL/yy h:mm a", Locale.US)

  "DecoderSpec" should "correctly parse the list of TicketStatusStatistics" in {
    val expectedResult = List(
      TicketStatusStatistics(
        User("sbarouski", "Siarhei Barouski"),
        Map(
          TicketStatus("In Progress") -> List(
            DatePeriod(simpleDateFormat.parse("08/Jan/20 5:54 PM"), simpleDateFormat.parse("08/Jan/20 6:05 PM")),
            DatePeriod(simpleDateFormat.parse("09/Jan/20 2:11 PM"), simpleDateFormat.parse("29/Jan/20 2:26 PM")),
            DatePeriod(simpleDateFormat.parse("29/Jan/20 2:32 PM"), simpleDateFormat.parse("29/Jan/20 2:52 PM"))
          ),
          TicketStatus("Open") -> List(
            DatePeriod(simpleDateFormat.parse("08/Jan/20 5:54 PM"), simpleDateFormat.parse("08/Jan/20 5:54 PM")),
            DatePeriod(simpleDateFormat.parse("08/Jan/20 6:05 PM"), simpleDateFormat.parse("09/Jan/20 2:11 PM")),
          ),
        )
      ),
      TicketStatusStatistics(
        User("intbacklog", "INT Backlog"),
        Map(
          TicketStatus("Open") -> List(
            DatePeriod(simpleDateFormat.parse("12/Aug/19 12:59 PM"), simpleDateFormat.parse("25/Nov/19 5:20 PM")),
            DatePeriod(simpleDateFormat.parse("27/Nov/19 11:12 AM"), simpleDateFormat.parse("02/Jan/20 11:24 AM")),
            DatePeriod(simpleDateFormat.parse("06/Jan/20 5:40 PM"), simpleDateFormat.parse("08/Jan/20 5:54 PM"))
          ),
          TicketStatus("Backlog") -> List(
            DatePeriod(simpleDateFormat.parse("25/Nov/19 5:20 PM"), simpleDateFormat.parse("27/Nov/19 11:12 AM")),
          ),
        )
      )
    )
    import Decoder.listTicketStatusStatisticsDecoder
    loadResource("decoder/correct_list.html")
      .toEntity[List[TicketStatusStatistics]].right.value should be(expectedResult)
  }

  it should "return HtmlDeserializationError -> PathNotFoundException if element not found" in {
    import Decoder.listTicketStatusStatisticsDecoder
    loadResource("decoder/incorrect_path_list.html")
      .toEntity[List[TicketStatusStatistics]].left.value should matchPattern {
      case HtmlDeserializationError(PathNotFoundException(_)) =>
    }
  }

  it should "return HtmlDeserializationError -> ParseException if date is in the wrong format" in {
    import Decoder.listTicketStatusStatisticsDecoder
    loadResource("decoder/incorrect_date_list.html")
      .toEntity[List[TicketStatusStatistics]].left.value should matchPattern {
      case HtmlDeserializationError(_: ParseException) =>
    }
  }

  it should "correctly parse the TicketStatusStatistics" in {
    val expectedResult = TicketStatusStatistics(
      User("sbarouski", "Siarhei Barouski"),
      Map(
        TicketStatus("In Progress") -> List(
          DatePeriod(simpleDateFormat.parse("08/Jan/20 5:54 PM"), simpleDateFormat.parse("08/Jan/20 6:05 PM")),
          DatePeriod(simpleDateFormat.parse("09/Jan/20 2:11 PM"), simpleDateFormat.parse("29/Jan/20 2:26 PM")),
          DatePeriod(simpleDateFormat.parse("29/Jan/20 2:32 PM"), simpleDateFormat.parse("29/Jan/20 2:52 PM"))
        ),
        TicketStatus("Open") -> List(
          DatePeriod(simpleDateFormat.parse("08/Jan/20 5:54 PM"), simpleDateFormat.parse("08/Jan/20 5:54 PM")),
          DatePeriod(simpleDateFormat.parse("08/Jan/20 6:05 PM"), simpleDateFormat.parse("09/Jan/20 2:11 PM")),
        ),
      )
    )
    import Decoder.ticketStatusStatisticsDecoder
    loadResource("decoder/correct_entity.html")
      .toEntity[TicketStatusStatistics].right.value should be(expectedResult)
  }

  it should "correctly parse the TicketStatusStatistics #2" in {
    val expectedResult = TicketStatusStatistics(
      User("sbarouski", "sbarouski"),
      Map(
        TicketStatus("In Progress") -> List(
          DatePeriod(simpleDateFormat.parse("08/Jan/20 5:54 PM"), simpleDateFormat.parse("08/Jan/20 6:05 PM")),
          DatePeriod(simpleDateFormat.parse("09/Jan/20 2:11 PM"), simpleDateFormat.parse("29/Jan/20 2:26 PM")),
          DatePeriod(simpleDateFormat.parse("29/Jan/20 2:32 PM"), simpleDateFormat.parse("29/Jan/20 2:52 PM"))
        ),
        TicketStatus("Open") -> List(
          DatePeriod(simpleDateFormat.parse("08/Jan/20 5:54 PM"), simpleDateFormat.parse("08/Jan/20 5:54 PM")),
          DatePeriod(simpleDateFormat.parse("08/Jan/20 6:05 PM"), simpleDateFormat.parse("09/Jan/20 2:11 PM")),
        ),
      )
    )
    import Decoder.ticketStatusStatisticsDecoder
    loadResource("decoder/correct_entity_2.html")
      .toEntity[TicketStatusStatistics].right.value should be(expectedResult)
  }

  private def loadResource(filename: String): String =
    scala.io.Source.fromResource(filename).mkString
}
