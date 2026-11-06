package com.example.talynxa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.talynxa.ui.NoteScreen
import com.example.talynxa.ui.NoteViewModel
import com.example.talynxa.ui.theme.TalynxaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent (e.g. file open via Intent.ACTION_VIEW or ACTION_SEND)
        viewModel.loadFromIntent(this, intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            val isDarkTheme = uiState.isDarkThemeOverride ?: isSystemInDarkTheme()

            // SAF Document Open launcher
            val openDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { viewModel.loadFromUri(context, it) }
            }

            // SAF Create Document (Save As) launcher
            val createDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let { viewModel.saveToUri(context, it) }
            }

            TalynxaTheme(
                darkTheme = isDarkTheme,
                oledMode = uiState.isOledTheme
            ) {
                NoteScreen(
                    viewModel = viewModel,
                    onOpenDocumentLauncher = {
                        openDocumentLauncher.launch(
                            arrayOf("application/json", "text/*", "*/*")
                        )
                    },
                    onCreateDocumentLauncher = {
                        val initialName = uiState.currentNote.fileName.ifBlank { "note.json" }
                        createDocumentLauncher.launch(initialName)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.loadFromIntent(this, intent)
    }
}
