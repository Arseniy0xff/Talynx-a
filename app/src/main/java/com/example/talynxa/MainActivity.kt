package com.example.talynxa

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import com.example.talynxa.ui.NoteViewModelFactory
import com.example.talynxa.ui.theme.TalynxaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(application)
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check if permission is granted after returning from Settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            // User didn't grant permission, but app remains functional via SAF
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check All Files Access on Android 11+ (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStorageLauncher.launch(intent)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageStorageLauncher.launch(intent)
                    } catch (_: Exception) {
                    }
                }
            }
        }

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
                        val initialName = "${uiState.currentNote.name.ifBlank { "note" }}.json"
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
