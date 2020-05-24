package com.eg.timeTrackingHelper.service.validation.exception

import cats.data.NonEmptyList

case class ValidationException(listError: NonEmptyList[String]) extends Exception
