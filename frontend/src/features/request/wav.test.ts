import { expect, test } from 'vitest'
import { encodeWav } from './wav'

test('encodes 48 kHz float samples as 16 kHz mono signed 16-bit PCM WAV', () => {
  const input = new Float32Array(48_000).fill(0.5)
  const buffer = encodeWav(input, 48_000)
  const view = new DataView(buffer)
  const tag = (offset: number) => String.fromCharCode(...new Uint8Array(buffer, offset, 4))
  expect(tag(0)).toBe('RIFF')
  expect(tag(8)).toBe('WAVE')
  expect(tag(12)).toBe('fmt ')
  expect(tag(36)).toBe('data')
  expect(view.getUint32(4, true)).toBe(buffer.byteLength - 8)
  expect(view.getUint32(16, true)).toBe(16)
  expect(view.getUint16(20, true)).toBe(1)
  expect(view.getUint16(22, true)).toBe(1)
  expect(view.getUint32(24, true)).toBe(16_000)
  expect(view.getUint32(28, true)).toBe(32_000)
  expect(view.getUint16(32, true)).toBe(2)
  expect(view.getUint16(34, true)).toBe(16)
  expect(view.getUint32(40, true)).toBe(32_000)
  expect(buffer.byteLength).toBe(32_044)
  expect(view.getInt16(44, true)).toBe(16_384)
})

test('averages source samples and clamps out-of-range PCM', () => {
  const view = new DataView(encodeWav(new Float32Array([1, 0, -1, -2, -2, -2, 2, 2, 2]), 48_000))
  expect(view.getInt16(44, true)).toBe(0)
  expect(view.getInt16(46, true)).toBe(-32768)
  expect(view.getInt16(48, true)).toBe(32767)
})

test('caps audio at 60 seconds and supports non-integer downsampling ratios', () => {
  expect(encodeWav(new Float32Array(44_100), 44_100).byteLength).toBe(32_044)
  expect(encodeWav(new Float32Array(16_000 * 61), 16_000).byteLength).toBe(44 + 60 * 32_000)
})
