package com.example.talynxa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import com.example.talynxa.ui.theme.TalynxaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTopAppBar(
    noteTitle: String,
    hasUriAttached: Boolean,
    modifier: Modifier = Modifier,
    isModified: Boolean = false,
    isOledTheme: Boolean = false,
    isDarkThemeOverride: Boolean? = null,
    onOpenClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onSaveAsClick: () -> Unit = {},
    onEditDetailsClick: () -> Unit = {},
    onExportJsonClick: () -> Unit = {},
    onToggleOled: () -> Unit = {},
    onSetDarkThemeOverride: (Boolean?) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (noteTitle.isBlank()) "Untitled Note" else noteTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Status indicator badge
                    Surface(
                        color = if (isModified) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isModified) MaterialTheme.colorScheme.error else Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isModified) "Unsaved" else if (hasUriAttached) "Saved" else "Local",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isModified) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenClick) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Open file"
                )
            }

            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save file"
                )
            }

            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Note Title") },
                    onClick = {
                        menuExpanded = false
                        onEditDetailsClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Save As...") },
                    onClick = {
                        menuExpanded = false
                        onSaveAsClick()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Export JSON") },
                    onClick = {
                        menuExpanded = false
                        onExportJsonClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(if (isOledTheme) "OLED Black: ON" else "OLED Black: OFF")
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleOled()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Contrast, contentDescription = null)
                    }
                )

                val themeText = when (isDarkThemeOverride) {
                    true -> "Theme: Force Dark"
                    false -> "Theme: Force Light"
                    null -> "Theme: System Default"
                }

                DropdownMenuItem(
                    text = { Text(themeText) },
                    onClick = {
                        menuExpanded = false
                        val nextOverride = when (isDarkThemeOverride) {
                            null -> true
                            true -> false
                            false -> null
                        }
                        onSetDarkThemeOverride(nextOverride)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Brightness4, contentDescription = null)
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun NoteTopAppBarPreview() {
    TalynxaTheme {
        NoteTopAppBar(
            noteTitle = "My Shopping Note",
            hasUriAttached = true,
            isModified = true,
            isOledTheme = false,
            isDarkThemeOverride = null
        )
    }
}
