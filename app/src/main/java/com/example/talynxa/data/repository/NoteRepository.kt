package com.example.talynxa.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.talynxa.data.model.NoteFile
import com.example.talynxa.data.model.NoteItem
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class NoteRepository(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }
) {

    fun getDefaultNote(): NoteFile {
        return NoteFile(
            name = "Заметки / Notes",
            fileName = "sample.json",
            content = listOf(
                NoteItem(
                    text = "Привет! Это примечание с поддержкой UTF-8 и кириллицы ✨",
                    tags = listOf("приветствие", "тест", "utf8")
                ),
                NoteItem(
                    text = "Organize your tasks and thoughts with tags.",
                    tags = listOf("info", "tags")
                ),
                NoteItem(
                    text = "Select multiple tags to filter items by match mode (ALL vs ANY).",
                    tags = listOf("info", "filter", "tips")
                )
            )
        )
    }

    fun parseJsonString(jsonString: String): Result<NoteFile> {
        return runCatching {
            json.decodeFromString(NoteFile.serializer(), jsonString)
        }
    }

    fun encodeJsonString(noteFile: NoteFile): String {
        return json.encodeToString(NoteFile.serializer(), noteFile)
    }

    fun readNoteFromInputStream(inputStream: InputStream): Result<NoteFile> {
        return runCatching {
            val jsonText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            json.decodeFromString(NoteFile.serializer(), jsonText)
        }
    }

    fun writeNoteToOutputStream(outputStream: OutputStream, noteFile: NoteFile): Result<Unit> {
        return runCatching {
            val jsonText = encodeJsonString(noteFile)
            outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(jsonText) }
        }
    }

    fun readNoteFromFile(file: File): Result<NoteFile> {
        return runCatching {
            val jsonText = file.readText(Charsets.UTF_8)
            json.decodeFromString(NoteFile.serializer(), jsonText)
        }
    }

    fun writeNoteToFile(file: File, noteFile: NoteFile): Result<Unit> {
        return runCatching {
            val jsonText = encodeJsonString(noteFile)
            file.writeText(jsonText, Charsets.UTF_8)
        }
    }

    fun readNoteFromUri(context: Context, uri: Uri): Result<NoteFile> {
        return runCatching {
            // Check if URI is a file scheme or file path
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        return readNoteFromFile(file)
                    }
                }
            }
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
            readNoteFromInputStream(inputStream).getOrThrow()
        }
    }

    fun writeNoteToUri(context: Context, uri: Uri, noteFile: NoteFile): Result<Unit> {
        return runCatching {
            // Check if URI is a file scheme or file path
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val file = File(path)
                    return writeNoteToFile(file, noteFile)
                }
            }
            val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                ?: context.contentResolver.openOutputStream(uri)
                ?: throw IllegalArgumentException("Cannot open output stream for URI: $uri")
            writeNoteToOutputStream(outputStream, noteFile).getOrThrow()
        }
    }

    fun readNoteFromIntent(context: Context, intent: Intent?): NoteFile {
        if (intent == null) return getDefaultNote()

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (_: Exception) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                        }
                    }
                    readNoteFromUri(context, uri).getOrNull()?.let { return it }
                }
            }
            Intent.ACTION_SEND -> {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: intent.data

                if (uri != null) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (_: Exception) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                        }
                    }
                    readNoteFromUri(context, uri).getOrNull()?.let { return it }
                } else {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!text.isNullOrBlank()) {
                        parseJsonString(text).getOrNull()?.let { return it }
                        // If plain text shared, wrap as a note item
                        return NoteFile(
                            name = "Shared Note",
                            fileName = "shared.json",
                            content = listOf(NoteItem(text = text, tags = listOf("shared")))
                        )
                    }
                }
            }
        }

        return getDefaultNote()
    }
}
