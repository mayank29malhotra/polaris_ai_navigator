package com.navigator.app.voice

/** Captures one spoken utterance and speaks the [respond] result. */
class VoiceSession(
    private val speech: SpeechInput,
    private val tts: TtsManager,
    private val respond: (String) -> String,
) {

    fun listen(onTranscript: (String) -> Unit = {}, onError: (Int) -> Unit = {}) {
        speech.listenOnce(
            onResult = { transcript ->
                onTranscript(transcript)
                tts.speak(respond(transcript))
            },
            onError = onError,
        )
    }
}
