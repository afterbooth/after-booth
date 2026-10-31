const SAMPLE_RATE = 16_000
export const MAX_RECORDING_SECONDS = 60

export function encodeWav(input: Float32Array, inputRate: number): ArrayBuffer {
  if (!Number.isFinite(inputRate) || inputRate < SAMPLE_RATE) {
    throw new Error('Unsupported sample rate')
  }
  const ratio = inputRate / SAMPLE_RATE
  const length = Math.min(Math.floor(input.length / ratio), SAMPLE_RATE * MAX_RECORDING_SECONDS)
  const buffer = new ArrayBuffer(44 + length * 2)
  const view = new DataView(buffer)
  const write = (offset: number, value: string) => {
    for (let i = 0; i < value.length; i++) view.setUint8(offset + i, value.charCodeAt(i))
  }
  write(0, 'RIFF')
  view.setUint32(4, 36 + length * 2, true)
  write(8, 'WAVE')
  write(12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, SAMPLE_RATE, true)
  view.setUint32(28, SAMPLE_RATE * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  write(36, 'data')
  view.setUint32(40, length * 2, true)
  for (let i = 0; i < length; i++) {
    const start = Math.floor(i * ratio)
    const end = Math.floor((i + 1) * ratio)
    let sum = 0
    for (let j = start; j < end; j++) sum += input[j]
    const sample = Math.max(-1, Math.min(1, sum / (end - start)))
    view.setInt16(44 + i * 2, Math.round(sample * (sample < 0 ? 32768 : 32767)), true)
  }
  return buffer
}
