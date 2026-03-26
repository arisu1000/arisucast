package com.arisucast.core.media.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // 재생 시작 버퍼 임계값 축소: 기본 2500ms → 500ms
        // 팟캐스트 오디오(~128kbps)는 소량 버퍼만으로도 안정적으로 재생됨
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,          // 최소 유지 버퍼: 50s
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,          // 최대 버퍼: 50s
                500,   // 재생 시작에 필요한 최소 버퍼 (기본값 2500ms)
                1_000  // rebuffer 후 재생 재개 최소 버퍼 (기본값 5000ms)
            )
            .build()

        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .build()
    }
}
