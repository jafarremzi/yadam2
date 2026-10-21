package app.yadavar.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** پوشش نازک روی SpeechRecognizer سیستم. همه فراخوانی‌ها باید روی نخ اصلی باشند. */
class SpeechController(
    private val context: Context,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
    }

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        r.setRecognitionListener(listener)
        recognizer = r
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        r.startListening(intent)
    }

    /** پایان طبیعی: تشخیص‌دهنده آنچه را شنیده پردازش و نتیجه را برمی‌گرداند. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** لغو بدون نتیجه و آزادسازی میکروفون. */
    fun destroy() {
        recognizer?.apply {
            setRecognitionListener(null)
            cancel()
            destroy()
        }
        recognizer = null
    }

    private fun first(b: Bundle?): String? =
        b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            first(partialResults)?.let { callbacks.onPartial(it) }
        }

        override fun onResults(results: Bundle?) {
            val text = first(results)
            destroy()
            if (text.isNullOrBlank()) callbacks.onError("چیزی تشخیص داده نشد. دوباره تلاش کنید.")
            else callbacks.onFinal(text)
        }

        override fun onError(error: Int) {
            destroy()
            callbacks.onError(describe(error))
        }
    }

    private fun describe(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_NETWORK ->
            "اتصال اینترنت برقرار نیست یا ضعیف است. تشخیص گفتار فارسی ممکن است به اینترنت نیاز داشته باشد."
        SpeechRecognizer.ERROR_AUDIO -> "مشکل در دریافت صدا از میکروفون."
        SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
            "سرویس تشخیص گفتار در دسترس نیست. کمی بعد دوباره تلاش کنید."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "صدایی شنیده نشد. دوباره دکمه میکروفون را لمس کنید."
        SpeechRecognizer.ERROR_NO_MATCH -> "متوجه گفتار نشدم. واضح‌تر و دوباره بگویید."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
            "سرویس تشخیص گفتار مشغول است. چند لحظه بعد تلاش کنید."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز میکروفون داده نشده است."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "زبان فارسی روی سرویس تشخیص گفتار این دستگاه پشتیبانی یا نصب نشده است."
        else -> "خطا در تشخیص گفتار (کد $error)."
    }
}
