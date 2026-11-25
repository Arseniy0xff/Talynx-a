package com.example.talynxa.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.example.talynxa.ui.components.rememberReorderableLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
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

                        // Note Items List with high performance item keys and O(1) index lookup cache
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
                            // Precompute index map for O(1) lookup during reordering
                            val indexMap = remember(fullNoteItems) {
                                fullNoteItems.withIndex().associate { it.value.id to it.index }
                            }
                            val totalSize = fullNoteItems.size

                            val lazyListState = rememberLazyListState()
                            val reorderState = rememberReorderableLazyListState(
                                lazyListState = lazyListState,
                                onMove = { fromIndex, toIndex ->
                                    if (fromIndex in listItems.indices && toIndex in listItems.indices) {
                                        val fromItem = listItems[fromIndex]
                                        val toItem = listItems[toIndex]
                                        val fromFullIndex = fullNoteItems.indexOfFirst { it.id == fromItem.id }
                                        val toFullIndex = fullNoteItems.indexOfFirst { it.id == toItem.id }
                                        if (fromFullIndex != -1 && toFullIndex != -1) {
                                            viewModel.moveItem(fromFullIndex, toFullIndex)
                                        }
                                    }
                                }
                            )

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(
                                    items = listItems,
                                    key = { item -> item.id }
                                ) { item ->
                                    val fullIndex = indexMap[item.id] ?: 0
                                    val isFirst = fullIndex == 0
                                    val isLast = fullIndex == totalSize - 1
                                    val isDragging = reorderState.draggingItemKey == item.id

                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { dismissValue ->
                                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                viewModel.deleteItem(item.id)
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        gesturesEnabled = !isDragging,
                                        backgroundContent = {
                                            val direction = dismissState.dismissDirection
                                            val color = if (direction == SwipeToDismissBoxValue.StartToEnd || direction == SwipeToDismissBoxValue.EndToStart) {
                                                MaterialTheme.colorScheme.errorContainer
                                            } else {
                                                Color.Transparent
                                            }
                                            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                                color = color,
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 24.dp),
                                                    contentAlignment = alignment
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        val itemModifier = if (isDragging) {
                                            Modifier
                                                .zIndex(1f)
                                                .graphicsLayer {
                                                    translationY = reorderState.draggedDistance
                                                    shadowElevation = 8.dp.toPx()
                                                    scaleX = 1.03f
                                                    scaleY = 1.03f
                                                    alpha = 0.95f
                                                }
                                        } else {
                                            Modifier
                                        }

                                        NoteItemCard(
                                            item = item,
                                            onClick = { editingItem = item },
                                            modifier = itemModifier,
                                            isFirst = isFirst,
                                            isLast = isLast,
                                            isDragging = isDragging,
                                            onDragStart = { reorderState.onDragStart(item.id) },
                                            onDragEnd = { reorderState.onDragEnd() },
                                            onDragCancel = { reorderState.onDragCancel() },
                                            onDrag = { dragAmount -> reorderState.onDrag(dragAmount) }
                                        )
                                    }
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
            onSaveItem = { text, tags, links ->
                if (text.isBlank() && tags.isEmpty()) {
                    viewModel.deleteItem(item.id)
                } else {
                    viewModel.editItem(item.id, text, tags, links)
                }
            },
            onDeleteItem = {
                viewModel.deleteItem(item.id)
            },
            allContent = uiState.currentNote.content,
            onNavigateToItem = { targetItem ->
                editingItem = targetItem
            }
        )
    }

    // New Item Dialog
    if (showNewItemDialog) {
        NoteEditDialog(
            item = null,
            onDismiss = { showNewItemDialog = false },
            onSaveItem = { text, tags, links ->
                if (text.isNotBlank() || tags.isNotEmpty()) {
                    viewModel.addItem(text, tags, links)
                }
            }
        )
    }

    if (showEditDetailsDialog) {
        NoteDetailsDialog(
            initialTitle = uiState.currentNote.name,
            onDismiss = { showEditDetailsDialog = false },
            onSaveDetails = { name ->
                viewModel.updateNoteDetails(name)
            }
        )
    }
}
