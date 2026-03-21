package com.arisucast.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import com.arisucast.core.database.entity.EpisodeEntity
import com.arisucast.core.database.entity.PodcastEntity
import com.arisucast.core.database.entity.SubscriptionEntity

@Database(
    entities = [
        PodcastEntity::class,
        EpisodeEntity::class,
        SubscriptionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ArisuCastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        const val DATABASE_NAME = "arisucast.db"
    }
}
