package com.glassinterface.core.memory

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for all memory entities.
 */
@Dao
interface MemoryDao {

    // ── Faces ────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFace(face: SavedFaceEntity): Long

    @Query("SELECT * FROM saved_faces ORDER BY createdAt DESC")
    fun getAllFaces(): Flow<List<SavedFaceEntity>>

    @Query("SELECT * FROM saved_faces")
    suspend fun getAllFacesSnapshot(): List<SavedFaceEntity>

    @Delete
    suspend fun deleteFace(face: SavedFaceEntity)

    // ── Objects ──────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(obj: SavedObjectEntity): Long

    @Query("SELECT * FROM saved_objects ORDER BY createdAt DESC")
    fun getAllObjects(): Flow<List<SavedObjectEntity>>

    @Delete
    suspend fun deleteObject(obj: SavedObjectEntity)

    // ── Contacts ─────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    // ── Locations ────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Query("SELECT * FROM locations ORDER BY createdAt DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    // ── Timestamps ───────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimestamp(ts: TimestampEntity): Long

    @Query("SELECT * FROM timestamps ORDER BY createdAt DESC")
    fun getAllTimestamps(): Flow<List<TimestampEntity>>

    @Delete
    suspend fun deleteTimestamp(ts: TimestampEntity)

    // ── Notes ────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // ── Summary (for "list memories" voice command) ──────────────────
    @Query("SELECT COUNT(*) FROM saved_faces")
    suspend fun faceCount(): Int

    @Query("SELECT COUNT(*) FROM saved_objects")
    suspend fun objectCount(): Int

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun contactCount(): Int

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun locationCount(): Int

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun noteCount(): Int
}
