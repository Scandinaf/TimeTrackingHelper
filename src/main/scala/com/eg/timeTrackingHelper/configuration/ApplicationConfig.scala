package com.eg.timeTrackingHelper.configuration

import com.eg.timeTrackingHelper.configuration.model.{
  ApplicationSettings,
  JiraConfig,
  Oauth2Settings,
  OutlookConfig,
  ServerSettings
}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object ApplicationConfig {
  private val source = ConfigSource.defaultApplication

  val jiraConfig = source
    .at("jira.configuration")
    .loadOrThrow[JiraConfig]

  val applicationSettings = source
    .at("application-settings")
    .loadOrThrow[ApplicationSettings]

  def outlookConfig =
    source
      .at("outlook.configuration")
      .loadOrThrow[OutlookConfig]

  def serverSettings =
    source
      .at("server-settings")
      .loadOrThrow[ServerSettings]

  def oauth2Settings =
    source
      .at("oauth2-settings")
      .loadOrThrow[Oauth2Settings]
}
