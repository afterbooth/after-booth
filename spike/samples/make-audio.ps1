# Regenerates the Korean TTS clips listed in audio.json as RIFF mono 16-bit PCM 16 kHz WAV.
# Run from anywhere: powershell -File spike/samples/make-audio.ps1
Add-Type -AssemblyName System.Speech
$dir = $PSScriptRoot
$spec = Get-Content -Raw -Encoding UTF8 (Join-Path $dir 'audio.json') | ConvertFrom-Json
$format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(16000, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, [System.Speech.AudioFormat.AudioChannel]::Mono)
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.SelectVoice('Microsoft Heami Desktop')
foreach ($clip in $spec.clips) {
  $path = Join-Path $dir $clip.file
  $synth.SetOutputToWaveFile($path, $format)
  $synth.Speak($clip.text)
  $synth.SetOutputToNull()
  Write-Output ("{0} {1} bytes" -f $clip.file, (Get-Item $path).Length)
}
$synth.Dispose()
