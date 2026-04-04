package com.glassinterface.core.memory

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database holding all persistent memory for GlassInterface.
 * Version 1: initial schema with faces, objects, contacts, locations, timestamps, notes.
 */
@Database(
    entities = [
        SavedFaceEntity::class,
        SavedObjectEntity::class,
        ContactEntity::class,
        LocationEntity::class,
        TimestampEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GlassDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
