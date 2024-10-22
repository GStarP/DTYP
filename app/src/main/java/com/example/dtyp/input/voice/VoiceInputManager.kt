package com.example.dtyp.input.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import kotlin.concurrent.thread
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.getEndpointConfig
import com.k2fsa.sherpa.onnx.getFeatureConfig

const val TAG = "DTYP"

class VoiceInputManager(private val context: Context) {
    private lateinit var recognizer: OnlineRecognizer
    private var audioRecord: AudioRecord? = null
    private var recognizingThread: Thread? = null
    private var onTextCallback: ((String) -> Unit)? = null

    @Volatile
    private var isRunning: Boolean = false

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_TYPE = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun start(onText: (String) -> Unit) {
        if (isRunning) {
            Log.w(TAG, "already running")
            return
        }
        Log.d(TAG, "start")
        initModel()
        startRecording()
        startRecognizing()
        onTextCallback = onText
        isRunning = true
    }

    fun stop() {
        if (!isRunning) {
            Log.w(TAG, "not running")
            return
        }
        Log.d(TAG, "stop")
        isRunning = false
        onTextCallback = null
        stopRecording()
        stopRecognizing()
        releaseModel()
    }

    private fun initModel() {
        Log.d(TAG, "initModel")
        val modelDir = "sherpa-onnx-streaming-zipformer-multi-zh-hans-2023-12-12"
        val modelConfig = OnlineModelConfig(
            transducer = OnlineTransducerModelConfig(
                encoder = "$modelDir/encoder-epoch-20-avg-1-chunk-16-left-128.int8.onnx",
                decoder = "$modelDir/decoder-epoch-20-avg-1-chunk-16-left-128.int8.onnx",
                joiner = "$modelDir/joiner-epoch-20-avg-1-chunk-16-left-128.int8.onnx",
            ),
            tokens = "$modelDir/tokens.txt",
            // https://github.com/k2-fsa/sherpa-onnx/issues/1236
            modelType = "zipformer2",
        )

        val config = OnlineRecognizerConfig(
            featConfig = getFeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            modelConfig = modelConfig,
            endpointConfig = getEndpointConfig(),
            enableEndpoint = true
        )

        recognizer = OnlineRecognizer(
            assetManager = context.assets,
            config = config,
        )
        Log.d(TAG, "initModel success")
    }

    private fun releaseModel() {
        recognizer.release()
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw Error("Permission denied: RECORD_AUDIO")
        }

        Log.d(TAG, "startRecording")
        val numBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_TYPE, AUDIO_FORMAT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_TYPE,
            AUDIO_FORMAT,
            numBytes * 2
        )
        audioRecord!!.startRecording()
    }

    private fun stopRecording() {
        Log.d(TAG, "stopRecording")
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun startRecognizing() {
        if (recognizingThread != null) {
            Log.e(TAG, "already recognizing")
            return
        }
        Log.d(TAG, "startRecognizing")
        recognizingThread = thread(true) { recognize() }
    }

    private fun stopRecognizing() {
        if (recognizingThread == null) {
            Log.e(TAG, "not recognizing")
            return
        }
        Log.d(TAG, "stopRecognizing")
        recognizingThread?.join()
        recognizingThread = null
    }

    private fun recognize() {
        var lastText: String = ""

        val stream = recognizer.createStream()
        // 每次处理 100ms 的音频数据
        val interval = 0.1
        val bufferSize = (interval * SAMPLE_RATE).toInt()
        val buffer = ShortArray(bufferSize)

        while (isRunning) {
            val ret = audioRecord?.read(buffer, 0, buffer.size)
            if (ret != null && ret > 0) {
                val samples = FloatArray(ret) { buffer[it] / 32768.0f }
                stream.acceptWaveform(samples, sampleRate = SAMPLE_RATE)
                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream)
                }

                val isEndpoint = recognizer.isEndpoint(stream)
                var text = recognizer.getResult(stream).text

                // https://github.com/k2-fsa/sherpa-onnx/blob/master/android/SherpaOnnx/app/src/main/java/com/k2fsa/sherpa/onnx/MainActivity.kt#L131
                // For streaming parformer, we need to manually add some
                // paddings so that it has enough right context to
                // recognize the last word of this segment
                if (isEndpoint && recognizer.config.modelConfig.paraformer.encoder.isNotBlank()) {
                    val tailPaddings = FloatArray((0.8 * SAMPLE_RATE).toInt())
                    stream.acceptWaveform(tailPaddings, sampleRate = SAMPLE_RATE)
                    while (recognizer.isReady(stream)) {
                        recognizer.decode(stream)
                    }
                    text = recognizer.getResult(stream).text
                }

                if (text.isNotBlank() && text != lastText) {
                    lastText = text
                    onTextCallback?.invoke(lastText)
                }
                // 识别到结束点后，重置识别流
                if (isEndpoint) {
                    recognizer.reset(stream)
                }
            }
        }

        stream.release()
    }
}
