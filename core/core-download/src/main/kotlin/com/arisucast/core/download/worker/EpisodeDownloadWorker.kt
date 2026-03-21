package com.arisucast.core.download.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arisucast.core.database.dao.EpisodeDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class EpisodeDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val episodeDao: EpisodeDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeId = inputData.getString(KEY_EPISODE_ID) ?: return@withContext Result.failure()
        val audioUrl = inputData.getString(KEY_AUDIO_URL) ?: return@withContext Result.failure()
        val episodeTitle = inputData.getString(KEY_EPISODE_TITLE) ?: "episode"

        val outputFile = File(
            applicationContext.getExternalFilesDir("podcasts"),
            "${episodeId}.mp3"
        )

        try {
            episodeDao.updateDownloadState(
                id = episodeId,
                status = "DOWNLOADING",
                path = null,
                workerId = id.toString(),
                downloadedAt = null
            )

            val request = Request.Builder().url(audioUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                episodeDao.updateDownloadState(episodeId, "FAILED", null, null, null)
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                episodeDao.updateDownloadState(episodeId, "FAILED", null, null, null)
                return@withContext Result.failure()
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloadedBytes += bytes

                        if (totalBytes > 0) {
                            val progress = (downloadedBytes * 100 / totalBytes).toInt()
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                }
            }

            episodeDao.updateDownloadState(
                id = episodeId,
                status = "DONE",
                path = outputFile.absolutePath,
                workerId = null,
                downloadedAt = System.currentTimeMillis()
            )

            Result.success()
        } catch (e: Exception) {
            outputFile.delete()
            episodeDao.updateDownloadState(episodeId, "FAILED", null, null, null)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_AUDIO_URL = "audio_url"
        const val KEY_EPISODE_TITLE = "episode_title"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
    }
}
