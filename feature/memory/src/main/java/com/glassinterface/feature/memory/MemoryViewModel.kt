package com.glassinterface.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassinterface.core.memory.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Memory browser screen.
 */
@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repository: MemoryRepository
) : ViewModel() {

    val faces = repository.getAllFaces().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val objects = repository.getAllObjects().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val contacts = repository.getAllContacts().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val locations = repository.getAllLocations().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val notes = repository.getAllNotes().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun deleteFace(face: SavedFaceEntity) { viewModelScope.launch { repository.deleteFace(face) } }
    fun deleteObject(obj: SavedObjectEntity) { viewModelScope.launch { repository.deleteObject(obj) } }
    fun deleteContact(contact: ContactEntity) { viewModelScope.launch { repository.deleteContact(contact) } }
    fun deleteLocation(loc: LocationEntity) { viewModelScope.launch { repository.deleteLocation(loc) } }
    fun deleteNote(note: NoteEntity) { viewModelScope.launch { repository.deleteNote(note) } }
}
