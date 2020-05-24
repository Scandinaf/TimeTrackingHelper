package com.eg.timeTrackingHelper.repository.jira.codec.exception

case class PathNotFoundException(path: String) extends Exception(s"Path - $path.")
