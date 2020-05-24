package com.eg.timeTrackingHelper.utils.converter

sealed trait ConvertException extends Exception {
  val innerException: Throwable
  super.initCause(innerException)
}

case class UnexpectableBehaviourException(innerException: Throwable) extends ConvertException