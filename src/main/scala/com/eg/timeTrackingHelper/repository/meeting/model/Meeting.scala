package com.eg.timeTrackingHelper.repository.meeting.model

import java.time.LocalDateTime

import scala.concurrent.duration.Duration

case class Meeting(
                    subject: Option[String],
                    isTakePlace: Boolean,
                    start: LocalDateTime,
                    end: LocalDateTime,
                    duration: Duration
                  ) {}
