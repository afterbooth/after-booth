import { afterEach, expect, test, vi } from 'vitest'
import { startRecording } from './recorder'

afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

function mockAudio() {
  const stop = vi.fn()
  const disconnect = vi.fn()
  const processor = {
    connect: vi.fn(),
    disconnect,
    onaudioprocess: null as
      ((event: { inputBuffer: { getChannelData: () => Float32Array } }) => void) | null,
  }
  const close = vi.fn().mockResolvedValue(undefined)
  class MockAudioContext {
    sampleRate = 48_000
    destination = {}
    resume = vi.fn().mockResolvedValue(undefined)
    close = close
    createMediaStreamSource = vi.fn(() => ({ connect: vi.fn(), disconnect }))
    createScriptProcessor = vi.fn(() => processor)
    createGain = vi.fn(() => ({ gain: { value: 1 }, connect: vi.fn(), disconnect }))
  }
  vi.stubGlobal('AudioContext', MockAudioContext)
  vi.stubGlobal('navigator', {
    mediaDevices: { getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop }] }) },
  })
  return { stop, close, processor }
}

test('captures PCM into a WAV file and releases the microphone on stop', async () => {
  const audio = mockAudio()
  const recording = await startRecording(vi.fn())
  audio.processor.onaudioprocess?.({
    inputBuffer: { getChannelData: () => new Float32Array(4800).fill(0.5) },
  })
  const file = recording.stop()
  expect(file.type).toBe('audio/wav')
  expect(file.name).toBe('pickup-request.wav')
  expect(file.size).toBe(44 + 1600 * 2)
  expect(audio.stop).toHaveBeenCalledOnce()
  expect(audio.close).toHaveBeenCalledOnce()
  expect(audio.processor.onaudioprocess).toBeNull()
})

test('stops at the wall-clock limit even when audio callbacks stall', async () => {
  vi.useFakeTimers()
  const audio = mockAudio()
  const onLimit = vi.fn(() => recording.cancel())
  const recording = await startRecording(onLimit)
  await vi.advanceTimersByTimeAsync(60_000)
  expect(onLimit).toHaveBeenCalledOnce()
  expect(audio.stop).toHaveBeenCalledOnce()
})

test('releases microphone tracks if AudioContext cannot start', async () => {
  const audio = mockAudio()
  vi.stubGlobal(
    'AudioContext',
    class {
      constructor() {
        throw new Error('Unavailable')
      }
    },
  )
  await expect(startRecording(vi.fn())).rejects.toThrow('Unavailable')
  expect(audio.stop).toHaveBeenCalledOnce()
})
