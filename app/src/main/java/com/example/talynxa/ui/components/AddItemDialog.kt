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
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAddItem: (text: String, tags: List<String>) -> Unit
) {
    var itemText by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Note Element") },
        text = {
            Column {
                OutlinedTextField(
                    value = itemText,
                    onValueChange = { itemText = it },
                    label = { Text("Content / Text") },
                    placeholder = { Text("Enter note details...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Initial Tags (comma separated)") },
                    placeholder = { Text("work, urgent, idea") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemText.isNotBlank()) {
                        val tags = tagsText
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onAddItem(itemText, tags)
                        onDismiss()
                    }
                },
                enabled = itemText.isNotBlank()
            ) {
                Text("Add Element")
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
private fun AddItemDialogPreview() {
    com.example.talynxa.ui.theme.TalynxaTheme {
        AddItemDialog(
            onDismiss = {},
            onAddItem = { _, _ -> }
        )
    }
}

