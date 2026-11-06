package com.example.talynxa.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NoteDetailsDialog(
    initialTitle: String,
    initialFileName: String,
    onDismiss: () -> Unit,
    onSaveDetails: (name: String, fileName: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var fileName by remember { mutableStateOf(initialFileName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note Info") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title (name)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (file_name)") },
                    placeholder = { Text("notes.json") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveDetails(title, fileName)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun NoteDetailsDialogPreview() {
    com.example.talynxa.ui.theme.TalynxaTheme {
        NoteDetailsDialog(
            initialTitle = "My Note Title",
            initialFileName = "my_file.json",
            onDismiss = {},
            onSaveDetails = { _, _ -> }
        )
    }
}

