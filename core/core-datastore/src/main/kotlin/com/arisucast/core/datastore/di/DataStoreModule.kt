package com.arisucast.core.datastore.di

import com.arisucast.core.datastore.UserPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    // UserPreferencesDataStore is @Singleton annotated and uses @Inject constructor
    // Hilt will automatically provide it
}
