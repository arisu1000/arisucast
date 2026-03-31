package com.arisucast.core.network.rss

import com.arisucast.core.common.result.Result
import com.rometools.modules.itunes.EntryInformation
import com.rometools.modules.itunes.FeedInformation
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.time.Instant
import javax.inject.Inject

private const val ITUNES_NS = "http://www.itunes.com/dtds/podcast-1.0.dtd"

data class ParsedPodcast(
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val websiteUrl: String,
    val language: String,
    val category: String,
    val episodes: List<ParsedEpisode>
)

data class ParsedEpisode(
    val guid: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val publishedAt: Instant,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val mimeType: String,
    val season: Int?,
    val episodeNumber: Int?
)

class RssParser @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun parseFeed(url: String): Result<ParsedPodcast> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.Error(
                    Exception("HTTP ${response.code}: ${response.message}"),
                    "피드를 불러올 수 없습니다. (${response.code})"
                )
            }

            val body = response.body
                ?: return@withContext Result.Error(Exception("Empty response body"), "응답이 비어있습니다.")

            val feed = SyndFeedInput().build(InputStreamReader(body.byteStream(), Charsets.UTF_8))
            Result.Success(feed.toParsedPodcast())
        } catch (e: Exception) {
            Result.Error(e, "RSS 파싱 오류: ${e.message}")
        }
    }

    private fun SyndFeed.toParsedPodcast(): ParsedPodcast {
        val itunesModule = getModule(ITUNES_NS) as? FeedInformation

        val imageUrl = itunesModule?.imageUri?.toString()
            ?: image?.url
            ?: ""

        val category = itunesModule?.categories?.firstOrNull()?.name
            ?: categories.firstOrNull()?.name
            ?: ""

        val author = itunesModule?.author
            ?: author?.trim()
            ?: managingEditor?.trim()
            ?: ""

        return ParsedPodcast(
            title = title?.trim() ?: "",
            author = author,
            description = itunesModule?.summary ?: description?.trim() ?: "",
            imageUrl = imageUrl,
            websiteUrl = link?.trim() ?: "",
            language = language?.trim() ?: "ko",
            category = category,
            episodes = entries.mapNotNull { it.toParsedEpisode(imageUrl) }
        )
    }

    private fun parseDurationToSeconds(durationString: String): Int {
        val parts = durationString.split(":")
        return when (parts.size) {
            3 -> {
                val hours = parts[0].toIntOrNull() ?: 0
                val minutes = parts[1].toIntOrNull() ?: 0
                val seconds = parts[2].toIntOrNull() ?: 0
                hours * 3600 + minutes * 60 + seconds
            }
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: 0
                val seconds = parts[1].toIntOrNull() ?: 0
                minutes * 60 + seconds
            }
            1 -> parts[0].toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun SyndEntry.toParsedEpisode(fallbackImageUrl: String): ParsedEpisode? {
        val enclosure = enclosures.firstOrNull { it.type?.startsWith("audio") == true }
            ?: return null

        val itunesEntry = getModule(ITUNES_NS) as? EntryInformation
        val durationSeconds = itunesEntry?.duration?.let { d ->
            parseDurationToSeconds(d.toString())
        } ?: 0

        val episodeImageUrl = itunesEntry?.image?.toString()
            ?: fallbackImageUrl

        val description = (contents.firstOrNull()?.value
            ?: itunesEntry?.summary
            ?: description?.value
            ?: "").trim()

        val guid = uri?.trim()?.takeIf { it.isNotBlank() }
            ?: link?.trim()?.takeIf { it.isNotBlank() }
            ?: enclosure.url

        return ParsedEpisode(
            guid = guid,
            title = title?.trim() ?: "",
            description = description,
            audioUrl = enclosure.url,
            imageUrl = episodeImageUrl,
            publishedAt = publishedDate?.toInstant()
                ?: updatedDate?.toInstant()
                ?: Instant.now(),
            durationSeconds = durationSeconds,
            fileSizeBytes = enclosure.length.takeIf { it > 0 } ?: 0L,
            mimeType = enclosure.type ?: "audio/mpeg",
            season = itunesEntry?.season?.takeIf { it > 0 },
            episodeNumber = itunesEntry?.episode?.takeIf { it > 0 }
        )
    }
}
