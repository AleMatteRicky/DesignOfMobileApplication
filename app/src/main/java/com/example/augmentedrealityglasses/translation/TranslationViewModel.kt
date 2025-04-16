package com.example.augmentedrealityglasses.translation

import android.Manifest
import android.R
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TranslationViewModel(
    private val systemLanguage: TranslateLanguage,
) : ViewModel() {
    var uiState by mutableStateOf(TranslationUiState())
        private set

    var recordJob: Job? =
        null //used to keep track of started recordJob in order to cancel them if the user starts a new recordJob before the old one finishes

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun record() {
        val recorder: AudioRecord = initializeAudioRecorder()
        recordJob?.cancel()
        recordJob = viewModelScope.launch{
            
        }
    }


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initializeAudioRecorder(): AudioRecord {

        val sampleRate = 32000 //32kHz
        val channelMask = AudioFormat.CHANNEL_IN_MONO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        return AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC) //set the device microphone has audio source
            .setAudioFormat(AudioFormat.Builder() //create and set and audio format
                .setEncoding(audioEncoding)
                .setSampleRate(sampleRate) //32kHz
                .setChannelMask(channelMask)
                .build())
            .setBufferSizeInBytes(2 * AudioRecord.getMinBufferSize(sampleRate, channelMask, audioEncoding))
            .build()
    }


}