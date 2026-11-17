package com.example.talynxa.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.talynxa.data.model.NoteItem
import com.example.talynxa.ui.components.NoteDetailsDialog
import com.example.talynxa.ui.components.NoteEditDialog
import com.example.talynxa.ui.components.NoteItemCard
import com.example.talynxa.ui.components.NoteTopAppBar
import com.example.talynxa.ui.components.TagFilterSection

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(
    viewModel: NoteViewModel,
    onOpenDocumentLauncher: () -> Unit,
    onCreateDocumentLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val tagCounts by viewModel.tagCounts.collectAsState()
    val filteredContent by viewModel.filteredContent.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var editingItem by remember { mutableStateOf<NoteItem?>(null) }
    var showNewItemDialog by remember { mutableStateOf(false) }
    var showEditDetailsDialog by remember { mutableStateOf(false) }

    // Handle Snackbar messages
    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearMessages()
        }
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NoteTopAppBar(
                noteTitle = uiState.currentNote.name,
                fileName = uiState.currentNote.fileName,
                hasUriAttached = uiState.currentUri != null,
                isModified = uiState.isModified,
                isOledTheme = uiState.isOledTheme,
                isDarkThemeOverride = uiState.isDarkThemeOverride,
                onOpenClick = onOpenDocumentLauncher,
                onSaveClick = {
                    if (uiState.currentUri != null) {
                        viewModel.saveToUri(
                            context = context,
                            uri = uiState.currentUri!!,
                            onPermissionFailure = onCreateDocumentLauncher
                        )
                    } else {
                        onCreateDocumentLauncher()
                    }
                },
                onSaveAsClick = onCreateDocumentLauncher,
                onEditDetailsClick = { showEditDetailsDialog = true },
                onExportJsonClick = {
                    val json = viewModel.exportToJsonString()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Talynxa Note JSON", json)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Exported JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                onToggleOled = { viewModel.toggleOledTheme() },
                onSetDarkThemeOverride = { override -> viewModel.setDarkThemeOverride(override) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Creating new item uses the same window/dialog.
                    // Empty text and tags upon closing/dismissing will discard/delete the item.
                    showNewItemDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Element") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Tag filter and search header
                        TagFilterSection(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            tagCounts = tagCounts,
                            selectedTags = uiState.selectedTags,
                            onTagToggle = { viewModel.toggleTagSelection(it) },
                            onClearTags = { viewModel.clearTagSelection() },
                            matchMode = uiState.matchMode,
                            onMatchModeChange = { viewModel.setMatchMode(it) },
                            totalItemsCount = uiState.currentNote.content.size,
                            filteredItemsCount = filteredContent.size
                        )

                        // Note Items List with reordering and drag support
                        if (filteredContent.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (uiState.currentNote.content.isEmpty())
                                            "No items in this note yet.\nTap '+ Add Element' to create one!"
                                        else
                                            "No items match the active search or filters.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            val listItems = filteredContent
                            val fullNoteItems = uiState.currentNote.content

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                itemsIndexed(
                                    items = listItems,
                                    key = { _, item -> item.id }
                                ) { _, item ->
                                    val fullIndex = fullNoteItems.indexOfFirst { it.id == item.id }
                                    val isFirst = fullIndex == 0
                                    val isLast = fullIndex == fullNoteItems.size - 1

                                    NoteItemCard(
                                        item = item,
                                        onClick = { editingItem = item },
                                        onDeleteItem = { id -> viewModel.deleteItem(id) },
                                        dragHandle = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.moveItemUp(item.id) },
                                                    enabled = !isFirst,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowUpward,
                                                        contentDescription = "Move Up",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.moveItemDown(item.id) },
                                                    enabled = !isLast,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDownward,
                                                        contentDescription = "Move Down",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Item Dialog
    editingItem?.let { item ->
        NoteEditDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSaveItem = { text, tags ->
                if (text.isBlank() && tags.isEmpty()) {
                    viewModel.deleteItem(item.id)
                } else {
                    viewModel.editItem(item.id, text, tags)
                }
            },
            onDeleteItem = {
                viewModel.deleteItem(item.id)
            }
        )
    }

    // New Item Dialog
    if (showNewItemDialog) {
        NoteEditDialog(
            item = null,
            onDismiss = { showNewItemDialog = false },
            onSaveItem = { text, tags ->
                if (text.isNotBlank() || tags.isNotEmpty()) {
                    viewModel.addItem(text, tags)
                }
                // If empty upon closing/dismissing, discard/delete (do nothing)
            }
        )
    }

    if (showEditDetailsDialog) {
        NoteDetailsDialog(
            initialTitle = uiState.currentNote.name,
            initialFileName = uiState.currentNote.fileName,
            onDismiss = { showEditDetailsDialog = false },
            onSaveDetails = { name, fileName ->
                viewModel.updateNoteDetails(name, fileName)
            }
        )
    }
}
