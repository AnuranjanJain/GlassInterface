package com.glassinterface.core.memory

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified repository for all persistent memory operations.
 * Handles bitmap saving to disk and embedding serialization.
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryDao,
    private val context: Context
) {
    companion object {
        private const val TAG = "MemoryRepository"
        private const val FACES_DIR = "saved_faces"
        private const val OBJECTS_DIR = "saved_objects"
    }

    // ── Face Operations ──────────────────────────────────────────────

    suspend fun saveFace(name: String, embedding: FloatArray, thumbnail: Bitmap?): Long {
        val thumbPath = thumbnail?.let { saveBitmap(it, FACES_DIR, "face_${System.currentTimeMillis()}") }
        val embeddingBytes = floatArrayToBytes(embedding)
        return dao.insertFace(SavedFaceEntity(name = name, embedding = embeddingBytes, thumbnailPath = thumbPath))
    }

    fun getAllFaces(): Flow<List<SavedFaceEntity>> = dao.getAllFaces()

    suspend fun getAllFacesSnapshot(): List<SavedFaceEntity> = dao.getAllFacesSnapshot()

    suspend fun deleteFace(face: SavedFaceEntity) {
        face.thumbnailPath?.let { File(it).delete() }
        dao.deleteFace(face)
    }

    /**
     * Find the closest matching face by cosine similarity.
     * Returns the name if similarity > threshold, else null.
     */
    suspend fun findFaceByEmbedding(queryEmbedding: FloatArray, threshold: Float = 0.75f): String? {
        val allFaces = dao.getAllFacesSnapshot()
        var bestMatch: String? = null
        var bestSimilarity = threshold

        for (face in allFaces) {
            val storedEmbedding = bytesToFloatArray(face.embedding)
            val similarity = cosineSimilarity(queryEmbedding, storedEmbedding)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestMatch = face.name
            }
        }

        return bestMatch
    }

    // ── Object Operations ────────────────────────────────────────────

    suspend fun saveObject(label: String, confidence: Float, thumbnail: Bitmap?, note: String? = null): Long {
        val thumbPath = thumbnail?.let { saveBitmap(it, OBJECTS_DIR, "obj_${System.currentTimeMillis()}") }
        return dao.insertObject(SavedObjectEntity(label = label, confidence = confidence, thumbnailPath = thumbPath, note = note))
    }

    fun getAllObjects(): Flow<List<SavedObjectEntity>> = dao.getAllObjects()

    suspend fun deleteObject(obj: SavedObjectEntity) {
        obj.thumbnailPath?.let { File(it).delete() }
        dao.deleteObject(obj)
    }

    // ── Contact Operations ───────────────────────────────────────────

    suspend fun saveContact(name: String, phone: String? = null, note: String? = null): Long =
        dao.insertContact(ContactEntity(name = name, phone = phone, note = note))

    fun getAllContacts(): Flow<List<ContactEntity>> = dao.getAllContacts()

    suspend fun deleteContact(contact: ContactEntity) = dao.deleteContact(contact)

    // ── Location Operations ──────────────────────────────────────────

    suspend fun saveLocation(label: String, latitude: Double, longitude: Double): Long =
        dao.insertLocation(LocationEntity(label = label, latitude = latitude, longitude = longitude))

    fun getAllLocations(): Flow<List<LocationEntity>> = dao.getAllLocations()

    suspend fun deleteLocation(location: LocationEntity) = dao.deleteLocation(location)

    // ── Timestamp Operations ─────────────────────────────────────────

    suspend fun saveTimestamp(label: String = "Timestamp", note: String? = null): Long =
        dao.insertTimestamp(TimestampEntity(label = label, note = note))

    fun getAllTimestamps(): Flow<List<TimestampEntity>> = dao.getAllTimestamps()

    suspend fun deleteTimestamp(ts: TimestampEntity) = dao.deleteTimestamp(ts)

    // ── Note Operations ──────────────────────────────────────────────

    suspend fun saveNote(content: String): Long = dao.insertNote(NoteEntity(content = content))

    fun getAllNotes(): Flow<List<NoteEntity>> = dao.getAllNotes()

    suspend fun deleteNote(note: NoteEntity) = dao.deleteNote(note)

    // ── Summary ──────────────────────────────────────────────────────

    suspend fun getMemorySummary(): String {
        val faces = dao.faceCount()
        val objects = dao.objectCount()
        val contacts = dao.contactCount()
        val locations = dao.locationCount()
        val notes = dao.noteCount()
        return "You have $faces saved faces, $objects saved objects, $contacts contacts, $locations locations, and $notes notes."
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private suspend fun saveBitmap(bitmap: Bitmap, subdir: String, filename: String): String =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, subdir)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$filename.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            Log.d(TAG, "Saved bitmap: ${file.absolutePath}")
            file.absolutePath
        }

    private fun floatArrayToBytes(arr: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(arr.size * 4)
        arr.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        return FloatArray(bytes.size / 4) { buffer.getFloat() }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = Math.sqrt((normA * normB).toDouble()).toFloat()
        return if (denom > 0f) dot / denom else 0f
    }
}
