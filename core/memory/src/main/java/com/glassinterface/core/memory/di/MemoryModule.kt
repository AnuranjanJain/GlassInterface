package com.glassinterface.core.memory.di

import android.content.Context
import androidx.room.Room
import com.glassinterface.core.memory.GlassDatabase
import com.glassinterface.core.memory.MemoryDao
import com.glassinterface.core.memory.MemoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MemoryModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GlassDatabase =
        Room.databaseBuilder(context, GlassDatabase::class.java, "glass_memory.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideMemoryDao(db: GlassDatabase): MemoryDao = db.memoryDao()

    @Provides
    @Singleton
    fun provideMemoryRepository(dao: MemoryDao, @ApplicationContext context: Context): MemoryRepository =
        MemoryRepository(dao, context)
}
