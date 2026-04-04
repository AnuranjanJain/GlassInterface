package com.glassinterface.core.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved face for re-identification.
 * [embedding] stores the face contour/landmark vector as a serialized FloatArray.
 * [thumbnailPath] is the absolute path to a cropped face JPEG on disk.
 */
@Entity(tableName = "saved_faces")
data class SavedFaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val embedding: ByteArray,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedFaceEntity) return false
        return id == other.id && name == other.name && embedding.contentEquals(other.embedding)
    }
    override fun hashCode(): Int = 31 * id.hashCode() + name.hashCode() + embedding.contentHashCode()
}

/**
 * A saved object snapshot (cropped detection with label).
 */
@Entity(tableName = "saved_objects")
data class SavedObjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val confidence: Float,
    val thumbnailPath: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A saved contact (name + optional phone).
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A saved location (GPS coordinates + label).
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A saved timestamp with an optional user note.
 */
@Entity(tableName = "timestamps")
data class TimestampEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "Timestamp",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A generic free-form note.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
