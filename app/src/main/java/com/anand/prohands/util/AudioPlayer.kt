package com.anand.prohands.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Audio player utility for playing voice messages.
 * Handles MediaPlayer lifecycle and provides playback state.
 */
class AudioPlayer private constructor() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingUrl = MutableStateFlow<String?>(null)
    val currentPlayingUrl: StateFlow<String?> = _currentPlayingUrl.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    companion object {
        private const val TAG = "AudioPlayer"

        @Volatile
        private var instance: AudioPlayer? = null

        fun getInstance(context: Context): AudioPlayer {
            // We don't actually need the context for MediaPlayer.setDataSource(url)
            return instance ?: synchronized(this) {
                instance ?: AudioPlayer().also { instance = it }
            }
        }
    }

    /**
     * Play audio from URL. If same URL is playing, toggle pause/resume.
     */
    fun playOrPause(url: String) {
        val safeUrl = url.replace("http://", "https://")

        if (currentUrl == safeUrl && mediaPlayer != null) {
            // Same audio - toggle play/pause
            if (mediaPlayer?.isPlaying == true) {
                pause()
            } else {
                resume()
            }
        } else {
            // New audio - stop current and play new
            stop()
            play(safeUrl)
        }
    }

    private fun play(url: String) {
        try {
            Log.d(TAG, "Playing audio: $url")

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)

                setOnPreparedListener { mp ->
                    _duration.value = mp.duration
                    mp.start()
                    _isPlaying.value = true
                    _currentPlayingUrl.value = url
                    Log.d(TAG, "Playback started, duration: ${mp.duration}ms")
                }

                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    _isPlaying.value = false
                    _progress.value = 0f
                    _currentPlayingUrl.value = null
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Playback error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    _currentPlayingUrl.value = null
                    true
                }

                prepareAsync()
            }

            currentUrl = url

        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            _isPlaying.value = false
        }
    }

    private fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            Log.d(TAG, "Playback paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    private fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            Log.d(TAG, "Playback resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            currentUrl = null
            _isPlaying.value = false
            _currentPlayingUrl.value = null
            _progress.value = 0f
            Log.d(TAG, "Playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
        }
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getDurationMs(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun formatDuration(millis: Int): String {
        val seconds = millis / 1000
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }

    fun release() {
        stop()
        instance = null
    }
}

