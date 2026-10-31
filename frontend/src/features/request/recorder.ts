import { encodeWav, MAX_RECORDING_SECONDS } from './wav'

export async function startRecording(onLimit: () => void) {
  const stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1 } })
  let context: AudioContext | undefined
  try {
    context = new AudioContext()
    await context.resume()
    const source = context.createMediaStreamSource(stream)
    // ScriptProcessor keeps the PCM capture local without a separately hosted worklet module.
    const processor = context.createScriptProcessor(4096, 1, 1)
    const mute = context.createGain()
    mute.gain.value = 0
    const chunks: Float32Array[] = []
    const maxSamples = context.sampleRate * MAX_RECORDING_SECONDS
    let samples = 0
    let stopped = false
    processor.onaudioprocess = (event) => {
      if (stopped) return
      const data = event.inputBuffer.getChannelData(0)
      const chunk = data.slice(0, Math.max(0, maxSamples - samples))
      chunks.push(chunk)
      samples += chunk.length
      if (samples >= maxSamples) onLimit()
    }
    source.connect(processor)
    processor.connect(mute)
    mute.connect(context.destination)
    const sampleRate = context.sampleRate
    const close = () => {
      stopped = true
      clearTimeout(timer)
      processor.onaudioprocess = null
      source.disconnect()
      processor.disconnect()
      mute.disconnect()
      stream.getTracks().forEach((track) => track.stop())
      void context?.close()
    }
    const timer = setTimeout(onLimit, MAX_RECORDING_SECONDS * 1000)
    return {
      cancel: close,
      stop: () => {
        close()
        const pcm = new Float32Array(samples)
        let offset = 0
        for (const chunk of chunks) {
          pcm.set(chunk, offset)
          offset += chunk.length
        }
        if (!samples) throw new Error('Empty recording')
        return new File([encodeWav(pcm, sampleRate)], 'pickup-request.wav', { type: 'audio/wav' })
      },
    }
  } catch (error) {
    stream.getTracks().forEach((track) => track.stop())
    void context?.close()
    throw error
  }
}
