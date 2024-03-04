package org.soundsofscala

import org.soundsofscala.models.*
import org.soundsofscala.models.AtomicMusicalEvent.*
import org.soundsofscala.models.DrumVoice.*
import org.soundsofscala.models.Duration.*
import org.soundsofscala.models.Velocity.*
import org.soundsofscala.models.AtomicMusicalEvent.DrumStroke
import org.soundsofscala.models.DrumVoice.{Kick, Snare}
import org.soundsofscala.models.Duration.Quarter
import org.soundsofscala.models.Velocity.OnFull

object TestData {
  val quarterKickDrum: DrumStroke = DrumStroke(Kick, Quarter, OnFull)
  val quarterSnareDrum: DrumStroke = DrumStroke(Snare, Quarter, OnFull)
  val RockBeatOne: Any =
    quarterKickDrum + quarterSnareDrum + quarterKickDrum + quarterSnareDrum

}
