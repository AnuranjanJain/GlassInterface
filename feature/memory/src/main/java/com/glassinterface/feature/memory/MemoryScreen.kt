package com.glassinterface.feature.memory

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glassinterface.core.memory.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Memory browser screen with tabbed UI for faces, objects, contacts, locations, and notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val faces by viewModel.faces.collectAsStateWithLifecycle()
    val objects by viewModel.objects.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Faces", "Objects", "Contacts", "Locations", "Notes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tab bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.White,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content
            when (selectedTab) {
                0 -> FacesList(faces) { viewModel.deleteFace(it) }
                1 -> ObjectsList(objects) { viewModel.deleteObject(it) }
                2 -> ContactsList(contacts) { viewModel.deleteContact(it) }
                3 -> LocationsList(locations) { viewModel.deleteLocation(it) }
                4 -> NotesList(notes) { viewModel.deleteNote(it) }
            }
        }
    }
}

@Composable
private fun FacesList(faces: List<SavedFaceEntity>, onDelete: (SavedFaceEntity) -> Unit) {
    if (faces.isEmpty()) { EmptyState("No saved faces yet.\nSay \"save face\" to save one.") ; return }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(faces, key = { it.id }) { face ->
            MemoryCard(
                title = face.name,
                subtitle = formatDate(face.createdAt),
                thumbnailPath = face.thumbnailPath,
                icon = Icons.Filled.Face,
                onDelete = { onDelete(face) }
            )
        }
    }
}

@Composable
private fun ObjectsList(objects: List<SavedObjectEntity>, onDelete: (SavedObjectEntity) -> Unit) {
    if (objects.isEmpty()) { EmptyState("No saved objects yet.\nSay \"save this\" to save one.") ; return }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(objects, key = { it.id }) { obj ->
            MemoryCard(
                title = "${obj.label} (${(obj.confidence * 100).toInt()}%)",
                subtitle = "${obj.note ?: ""} ${formatDate(obj.createdAt)}".trim(),
                thumbnailPath = obj.thumbnailPath,
                icon = Icons.Filled.CenterFocusStrong,
                onDelete = { onDelete(obj) }
            )
        }
    }
}

@Composable
private fun ContactsList(contacts: List<ContactEntity>, onDelete: (ContactEntity) -> Unit) {
    if (contacts.isEmpty()) { EmptyState("No saved contacts yet.\nSay \"save contact\" to add one.") ; return }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(contacts, key = { it.id }) { contact ->
            MemoryCard(
                title = contact.name,
                subtitle = "${contact.phone ?: "No phone"} · ${formatDate(contact.createdAt)}",
                icon = Icons.Filled.Person,
                onDelete = { onDelete(contact) }
            )
        }
    }
}

@Composable
private fun LocationsList(locations: List<LocationEntity>, onDelete: (LocationEntity) -> Unit) {
    if (locations.isEmpty()) { EmptyState("No saved locations yet.\nSay \"save location\" to save one.") ; return }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(locations, key = { it.id }) { loc ->
            MemoryCard(
                title = loc.label,
                subtitle = "${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)} · ${formatDate(loc.createdAt)}",
                icon = Icons.Filled.LocationOn,
                onDelete = { onDelete(loc) }
            )
        }
    }
}

@Composable
private fun NotesList(notes: List<NoteEntity>, onDelete: (NoteEntity) -> Unit) {
    if (notes.isEmpty()) { EmptyState("No saved notes yet.\nSay \"save note\" to add one.") ; return }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            MemoryCard(
                title = note.content,
                subtitle = formatDate(note.createdAt),
                icon = Icons.Filled.StickyNote2,
                onDelete = { onDelete(note) }
            )
        }
    }
}

@Composable
private fun MemoryCard(
    title: String,
    subtitle: String,
    thumbnailPath: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or icon
            if (thumbnailPath != null && File(thumbnailPath).exists()) {
                val bitmap = remember(thumbnailPath) {
                    BitmapFactory.decodeFile(thumbnailPath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF424242)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 2)
                Text(subtitle, color = Color(0xFFBDBDBD), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFF757575),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(32.dp)
        )
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
