package com.example.talynxa

import com.example.talynxa.data.model.NoteFile
import com.example.talynxa.data.model.NoteItem
import com.example.talynxa.data.repository.NoteRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class NoteRepositoryTest {

    private val repository = NoteRepository()

    @Test
    fun testJsonSchemaSerialization() {
        val jsonInput = """
            {
                "name": "Test",
                "file_name": "test",
                "content": [
                    {
                        "text": "test",
                        "tags": ["a", "b"]
                    }
                ]
            }
        """.trimIndent()

        val parseResult = repository.parseJsonString(jsonInput)
        assertTrue(parseResult.isSuccess)

        val noteFile = parseResult.getOrThrow()
        assertEquals("Test", noteFile.name)
        assertEquals("test", noteFile.fileName)
        assertEquals(1, noteFile.content.size)
        assertEquals("test", noteFile.content[0].text)
        assertEquals(listOf("a", "b"), noteFile.content[0].tags)

        val reEncoded = repository.encodeJsonString(noteFile)
        assertTrue(reEncoded.contains("\"name\": \"Test\""))
        assertTrue(reEncoded.contains("\"file_name\": \"test\""))
    }

    @Test
    fun testCyrillicAndUtf8Handling() {
        val originalNote = NoteFile(
            name = "Заметки на русском 🇷🇺",
            fileName = "notes_ru.json",
            content = listOf(
                NoteItem(
                    text = "Привет, мир! 🌍 Кириллические символы и эмодзи ✨",
                    tags = listOf("русский", "тест", "utf8")
                )
            )
        )

        val jsonString = repository.encodeJsonString(originalNote)
        val decodedNote = repository.parseJsonString(jsonString).getOrThrow()

        assertEquals(originalNote.name, decodedNote.name)
        assertEquals(originalNote.fileName, decodedNote.fileName)
        assertEquals(originalNote.content[0].text, decodedNote.content[0].text)
        assertEquals(originalNote.content[0].tags, decodedNote.content[0].tags)
    }

    @Test
    fun testStreamReadWrite() {
        val originalNote = repository.getDefaultNote()
        val outputStream = ByteArrayOutputStream()

        val writeResult = repository.writeNoteToOutputStream(outputStream, originalNote)
        assertTrue(writeResult.isSuccess)

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val readResult = repository.readNoteFromInputStream(inputStream)
        assertTrue(readResult.isSuccess)

        val readNote = readResult.getOrThrow()
        assertEquals(originalNote.name, readNote.name)
        assertEquals(originalNote.content.size, readNote.content.size)
    }
}
