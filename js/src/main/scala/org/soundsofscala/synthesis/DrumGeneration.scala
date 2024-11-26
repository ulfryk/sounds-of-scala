/*
 * Copyright 2024 Sounds of Scala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.soundsofscala.synthesis

import cats.effect.IO
import org.scalajs.dom
import org.scalajs.dom.AudioContext
import org.soundsofscala.models
import org.soundsofscala.models.AtomicMusicalEvent
import org.soundsofscala.models.AtomicMusicalEvent.DrumStroke
import org.soundsofscala.synthesis.DrumGeneration.Hats808Instruction.{Closed, Open}

/**
 * This is a POC of creating drum sounds using the Web Audio API. The sounds are based on the 808
 * drum machine. TODO: Refactor this to use a Synth API
 */
object DrumGeneration:

  def generateKick808(
      drumStroke: DrumStroke,
      when: Double
  )(using audioContext: AudioContext): IO[Unit] =
    IO:
      val velocity = drumStroke.velocity.getNormalisedVelocity
      val osc = audioContext.createOscillator()
      osc.`type` = "sine"
      osc.frequency.setValueAtTime(80, when)
      osc.frequency.exponentialRampToValueAtTime(30, when + 0.2)
      val gain = audioContext.createGain()

      val filterShelf = audioContext.createBiquadFilter()
      filterShelf.`type` = "lowshelf"
      filterShelf.frequency.value = 300
      filterShelf.gain.value = 5

      gain.gain.linearRampToValueAtTime(velocity * 0.5, when + 0.01)

      gain.gain.exponentialRampToValueAtTime(0.001, when + 0.9)

      osc.connect(filterShelf)
      filterShelf.connect(gain)
      gain.connect(audioContext.destination)
      osc.start(when)
      osc.stop(when + 0.9)

  def generateClap808(
      drumStroke: DrumStroke,
      when: Double
  )(using audioContext: AudioContext): IO[Unit] =
    IO:
      val velocity = drumStroke.velocity.getNormalisedVelocity

      def createNoiseBuffer(audioContext: dom.AudioContext): dom.AudioBuffer =
        val bufferSize = audioContext.sampleRate.toFloat // 1 second of audio
        val buffer =
          audioContext.createBuffer(1, bufferSize.toInt, audioContext.sampleRate.toInt)
        val output = buffer.getChannelData(0)
        (0 until output.length).foreach(i => output(i) = (math.random() * 2 - 1).toFloat)
        buffer

      val noiseBuffer = createNoiseBuffer(audioContext)
      val noiseSource = audioContext.createBufferSource()
      noiseSource.buffer = noiseBuffer

      val bandpass = audioContext.createBiquadFilter()
      bandpass.`type` = "bandpass"
      bandpass.frequency.value = 1000
      bandpass.Q.value = 0.2

      val highpass = audioContext.createBiquadFilter()
      highpass.`type` = "highpass"
      highpass.frequency.value = 2000

      val gain = audioContext.createGain()
      gain.gain.setValueAtTime(0, when)
      gain.gain.linearRampToValueAtTime(velocity, when + 0.01)
      gain.gain.exponentialRampToValueAtTime(0.01, when + 0.2)

      noiseSource.connect(bandpass)
      bandpass.connect(highpass)
      highpass.connect(gain)
      gain.connect(audioContext.destination)

      noiseSource.start(when)
      noiseSource.stop(when + 0.2)

  enum Hats808Instruction:
    case Closed
    case Open(duration: Double)

  def generateHats808(
      drumStroke: DrumStroke,
      when: Double,
      instr: Hats808Instruction
  )(using audioContext: AudioContext): IO[Unit] =
    IO:
      val velocity = drumStroke.velocity.getNormalisedVelocity
      val bufferSize = audioContext.sampleRate * 2.0
      val noiseBuffer =
        audioContext.createBuffer(1, bufferSize.toInt, audioContext.sampleRate.toInt)
      val output = noiseBuffer.getChannelData(0)
      (0 until output.length).foreach(i => output(i) = (math.random() * 2 - 1).toFloat)

      val noise = audioContext.createBufferSource()
      noise.buffer = noiseBuffer

      val filterFrequency = instr match
        case Closed => 5_000
        case Open(_) => 9_000
      val highPass = audioContext.createBiquadFilter()
      highPass.`type` = "highpass"
      highPass
        .frequency
        .setValueAtTime(filterFrequency, when) // High-pass filter frequency, adjust as needed

      val gain = audioContext.createGain()
      gain.gain.setValueAtTime(velocity, when) // Start at full volume
      instr match
        case Closed =>
          gain.gain.exponentialRampToValueAtTime(0.5, when + 0.1)
        case Open(duration) =>
          gain.gain.exponentialRampToValueAtTime(0.5, when + 0.1)
          gain.gain.exponentialRampToValueAtTime(0.1, when + duration * 0.8)

      noise.connect(highPass)
      highPass.connect(gain)
      gain.connect(audioContext.destination)

      noise.start(when)
      val noteRelease = instr match
        case Closed => 0.05
        case Open(duration) => duration * 0.9
      noise.stop(when + noteRelease)

  def generateSnare808(drumStroke: DrumStroke, when: Double)(
      using audioContext: AudioContext): IO[Unit] =
    IO:
      val velocity = drumStroke.velocity.getNormalisedVelocity

      val bodyOsc = audioContext.createOscillator()
      bodyOsc.`type` = "triangle"
      bodyOsc.frequency.setValueAtTime(170, when)

      val bodyGain = audioContext.createGain()
      bodyGain.gain.setValueAtTime(velocity, when)
      bodyGain.gain.linearRampToValueAtTime(0.01, when + 0.1)

      bodyOsc.connect(bodyGain)

      val bufferSize = audioContext.sampleRate.toInt * 2 // 2 seconds of audio
      val noiseBuffer =
        audioContext.createBuffer(1, bufferSize, audioContext.sampleRate.toInt)
      val output = noiseBuffer.getChannelData(0)

      (0 until output.length).foreach(i => output(i) = (math.random() * 2 - 1).toFloat)
      val noise = audioContext.createBufferSource()
      noise.buffer = noiseBuffer

      val noiseFilter = audioContext.createBiquadFilter()
      noiseFilter.`type` = "bandpass"
      noiseFilter.frequency.setValueAtTime(1000, when)

      noise.connect(noiseFilter)

      val noiseGain = audioContext.createGain()
      noiseGain.gain.setValueAtTime(velocity * .8, when)
      noiseGain.gain.linearRampToValueAtTime(0.01, when + 0.3) // Decay

      noiseFilter.connect(noiseGain)

      bodyGain.connect(audioContext.destination)
      noiseGain.connect(audioContext.destination)

      bodyOsc.start(when)
      noise.start(when)

      bodyOsc.stop(when + 0.3)
      noise.stop(when + 0.3)
end DrumGeneration
