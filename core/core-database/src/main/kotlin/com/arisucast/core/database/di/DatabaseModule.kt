package com.arisucast.core.database.di

import android.content.Context
import androidx.room.Room
import com.arisucast.core.database.ArisuCastDatabase
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArisuCastDatabase =
        Room.databaseBuilder(
            context,
            ArisuCastDatabase::class.java,
            ArisuCastDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun providePodcastDao(db: ArisuCastDatabase): PodcastDao = db.podcastDao()

    @Provides
    fun provideEpisodeDao(db: ArisuCastDatabase): EpisodeDao = db.episodeDao()

    @Provides
    fun provideSubscriptionDao(db: ArisuCastDatabase): SubscriptionDao = db.subscriptionDao()
}
