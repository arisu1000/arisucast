package com.arisucast.core.download.manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arisucast.core.download.worker.EpisodeDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun downloadEpisode(
        episodeId: String,
        audioUrl: String,
        episodeTitle: String,
        wifiOnly: Boolean = true
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = Data.Builder()
            .putString(EpisodeDownloadWorker.KEY_EPISODE_ID, episodeId)
            .putString(EpisodeDownloadWorker.KEY_AUDIO_URL, audioUrl)
            .putString(EpisodeDownloadWorker.KEY_EPISODE_TITLE, episodeTitle)
            .build()

        val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(episodeId)
            .build()

        workManager.enqueueUniqueWork(
            "download_$episodeId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelDownload(episodeId: String) {
        workManager.cancelUniqueWork("download_$episodeId")
    }

    fun getDownloadProgress(episodeId: String): Flow<Int> =
        workManager.getWorkInfosByTagFlow(episodeId).map { infos ->
            val info = infos.firstOrNull() ?: return@map -1
            when (info.state) {
                WorkInfo.State.RUNNING ->
                    info.progress.getInt(EpisodeDownloadWorker.KEY_PROGRESS, 0)
                WorkInfo.State.SUCCEEDED -> 100
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> -1
                else -> 0
            }
        }
}
