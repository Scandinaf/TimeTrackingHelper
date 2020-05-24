package com.eg.timeTrackingHelper.service.model

case class HierarchicalWorklog(
                                minor: List[WorklogEntity],
                                major: List[WorklogEntity]
                              )
