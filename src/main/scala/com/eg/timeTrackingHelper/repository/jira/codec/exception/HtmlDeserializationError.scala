package com.eg.timeTrackingHelper.repository.jira.codec.exception

case class HtmlDeserializationError(cause: Throwable) extends Exception(cause)
