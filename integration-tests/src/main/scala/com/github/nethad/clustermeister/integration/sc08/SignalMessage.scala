package com.github.nethad.clustermeister.integration.sc08

// algorithm-specific message
case class SignalMessage[@specialized SourceId, @specialized TargetId, @specialized SignalType](sourceId: SourceId, targetId: TargetId, signal: SignalType) {
  override def toString = "Signal(sourceId=" + sourceId + ", targetId=" + targetId + ", signal=" + signal + ")"
}

//case class SignalMessage[SourceId, TargetId, SignalType](sourceId: SourceId, targetId: TargetId, signal: SignalType) {
//  override def toString = "Signal(sourceId=" + sourceId + ", targetId=" + targetId + ", signal=" + signal + ")"
//}
