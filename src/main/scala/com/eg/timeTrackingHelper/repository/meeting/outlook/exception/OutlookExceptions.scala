package com.eg.timeTrackingHelper.repository.meeting.outlook.exception

sealed trait OutlookException extends Exception {
  val innerException: Throwable
  super.initCause(innerException)
}

case class UnauthorizedException(innerException: Throwable) extends OutlookException

case class ServerNotFoundException(innerException: Throwable) extends OutlookException

case class UnknownException(innerException: Throwable) extends OutlookException
