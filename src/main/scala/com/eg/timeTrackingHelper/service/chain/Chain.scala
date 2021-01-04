package com.eg.timeTrackingHelper.service.chain

private[chain] trait Chain[T] {

  def initialState: T

  def tasks: List[T => T]

  def canContinue(state: T): Boolean

  def execute: T =
    tasks.foldLeft(initialState)((state, function) =>
      if (canContinue(state)) function(state)
      else state
    )
}
