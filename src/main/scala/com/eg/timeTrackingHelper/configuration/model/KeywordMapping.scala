package com.eg.timeTrackingHelper.configuration.model

case class KeywordMapping(
                           defaultTicket: String,
                           keywordMappingByTicket: Map[String, Set[String]]
                         )
