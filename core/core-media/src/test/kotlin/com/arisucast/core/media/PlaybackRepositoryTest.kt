package com.arisucast.core.media

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.datastore.UserPreferencesDataStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val context: Context = mockk(relaxed = true)
    private val player: ExoPlayer = mockk(relaxed = true)
    private val episodeDao: EpisodeDao = mockk(relaxed = true)
    private val dataStore: UserPreferencesDataStore = mockk(relaxed = true)
    private lateinit var repository: PlaybackRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // restoreLastPlayingEpisode()가 init에서 호출되므로 null을 반환하도록 stub
        every { dataStore.lastPlayingEpisodeId } returns flowOf(null)
        repository = PlaybackRepository(context, player, episodeDao, dataStore)
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state - not playing, no current episode`() {
        val state = repository.state.value
        assertFalse(state.isPlaying)
        assertEquals(null, state.currentEpisode)
        assertEquals(0L, state.positionMs)
        assertFalse(state.sleepTimerActive)
    }

    @Test
    fun `sleep timer - activates timer and sets end time`() {
        repository.setSleepTimer(60_000L)

        val state = repository.state.value
        assertTrue(state.sleepTimerActive)
        assertTrue(state.sleepTimerEndMs > System.currentTimeMillis())
    }

    @Test
    fun `sleep timer - passing 0 cancels active timer`() {
        repository.setSleepTimer(60_000L)
        assertTrue(repository.state.value.sleepTimerActive)

        repository.setSleepTimer(0L)
        assertFalse(repository.state.value.sleepTimerActive)
        assertEquals(0L, repository.state.value.sleepTimerEndMs)
    }

    @Test
    fun `sleep timer - replacing with shorter duration updates end time`() {
        repository.setSleepTimer(3_600_000L)  // 1 hour
        val longEnd = repository.state.value.sleepTimerEndMs

        repository.setSleepTimer(900_000L)    // 15 min
        val shortEnd = repository.state.value.sleepTimerEndMs

        assertTrue(shortEnd < longEnd)
    }

    @Test
    fun `skipBack - seeks to max(0, position - ms)`() {
        every { player.currentPosition } returns 15_000L
        repository.skipBack(10_000L)
        verify { player.seekTo(5_000L) }
    }

    @Test
    fun `skipBack - clamps to 0 when position is less than skip amount`() {
        every { player.currentPosition } returns 3_000L
        repository.skipBack(10_000L)
        verify { player.seekTo(0L) }
    }

    @Test
    fun `skipForward - seeks forward by given ms`() {
        every { player.currentPosition } returns 10_000L
        every { player.duration } returns 120_000L
        repository.skipForward(30_000L)
        verify { player.seekTo(40_000L) }
    }

    @Test
    fun `skipForward - clamps to duration`() {
        every { player.currentPosition } returns 100_000L
        every { player.duration } returns 120_000L
        repository.skipForward(30_000L)
        verify { player.seekTo(120_000L) }
    }

    @Test
    fun `playPause - pauses when player is playing`() {
        every { player.isPlaying } returns true
        repository.playPause()
        verify { player.pause() }
    }

    @Test
    fun `playPause - plays when player is paused`() {
        every { player.isPlaying } returns false
        repository.playPause()
        verify { player.play() }
    }

    @Test
    fun `setPlaybackSpeed - updates player and state`() {
        repository.setPlaybackSpeed(1.5f)

        verify { player.setPlaybackSpeed(1.5f) }
        assertEquals(1.5f, repository.state.value.playbackSpeed)
    }

    @Test
    fun `seekToFraction - no-op when duration is zero or negative`() {
        every { player.duration } returns 0L
        repository.seekToFraction(0.5f)
        verify(exactly = 0) { player.seekTo(any()) }
    }

    @Test
    fun `seekToFraction - seeks to correct position based on duration`() {
        every { player.duration } returns 100_000L
        repository.seekToFraction(0.25f)
        verify { player.seekTo(25_000L) }
    }

    @Test
    fun `currentEpisodeId - returns null when no media item`() {
        every { player.currentMediaItem } returns null
        assertEquals(null, repository.currentEpisodeId())
    }
}
