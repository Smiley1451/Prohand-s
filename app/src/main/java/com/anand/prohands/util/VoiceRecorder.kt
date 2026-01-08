package com.anand.prohands.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Voice recorder utility for capturing audio messages.
 * Handles the MediaRecorder lifecycle and file management.
 */
class VoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    companion object {
        private const val TAG = "VoiceRecorder"
    }

    /**
     * Start recording audio to a temp file.
     * @return The file that will contain the recording, or null if failed.
     */
    fun startRecording(): File? {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return outputFile
        }

        try {
            // Create output file
            val recordingsDir = File(context.cacheDir, "voice_recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            outputFile = File(recordingsDir, "voice_${System.currentTimeMillis()}.m4a")

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Started recording to: ${outputFile?.absolutePath}")
            return outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            return null
        }
    }

    /**
     * Stop recording and return the recorded file.
     * @return The recorded file, or null if recording failed.
     */
    fun stopRecording(): File? {
        if (!isRecording) {
            Log.w(TAG, "Not currently recording")
            return null
        }

        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            // Verify file has content
            val file = outputFile
            if (file != null && file.exists() && file.length() > 0) {
                Log.d(TAG, "Recording saved: ${file.absolutePath}, size: ${file.length()}")
                file
            } else {
                Log.e(TAG, "Recording file is empty or doesn't exist")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            cleanup()
            null
        }
    }

    /**
     * Cancel the current recording and delete the file.
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore - might not have started properly
        }
        cleanup()
    }

    /**
     * Get the current amplitude for visualization.
     * @return Amplitude value (0-32767), or 0 if not recording.
     */
    fun getAmplitude(): Int {
        return if (isRecording) {
            try {
                mediaRecorder?.maxAmplitude ?: 0
            } catch (e: Exception) {
                0
            }
        } else {
            0
        }
    }

    /**
     * Check if currently recording.
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    private fun cleanup() {
        mediaRecorder = null
        isRecording = false
        // Delete incomplete file
        outputFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        outputFile = null
    }
}

