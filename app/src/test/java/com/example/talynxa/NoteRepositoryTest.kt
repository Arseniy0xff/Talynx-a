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
                "content": [
                    {
                        "id": 0,
                        "text": "test",
                        "tags": ["a", "b"],
                        "links": [1, "2", 3]
                    }
                ]
            }
        """.trimIndent()

        val parseResult = repository.parseJsonString(jsonInput)
        assertTrue(parseResult.isSuccess)

        val noteFile = parseResult.getOrThrow()
        assertEquals("Test", noteFile.name)
        assertEquals(1, noteFile.content.size)
        assertEquals("0", noteFile.content[0].id)
        assertEquals("test", noteFile.content[0].text)
        assertEquals(listOf("a", "b"), noteFile.content[0].tags)
        assertEquals(listOf("1", "2", "3"), noteFile.content[0].links)

        val reEncoded = repository.encodeJsonString(noteFile)
        assertTrue(reEncoded.contains("Test"))
        assertTrue(reEncoded.contains("0"))
        assertTrue(reEncoded.contains("1"))
    }

    @Test
    fun testCyrillicAndUtf8Handling() {
        val originalNote = NoteFile(
            name = "Заметки на русском 🇷🇺",
            content = listOf(
                NoteItem(
                    id = "0",
                    text = "Привет, мир! 🌍 Кириллические символы и эмодзи ✨",
                    tags = listOf("русский", "тест", "utf8"),
                    links = listOf("1")
                )
            )
        )

        val jsonString = repository.encodeJsonString(originalNote)
        val decodedNote = repository.parseJsonString(jsonString).getOrThrow()

        assertEquals(originalNote.name, decodedNote.name)
        assertEquals(originalNote.content[0].id, decodedNote.content[0].id)
        assertEquals(originalNote.content[0].text, decodedNote.content[0].text)
        assertEquals(originalNote.content[0].tags, decodedNote.content[0].tags)
        assertEquals(originalNote.content[0].links, decodedNote.content[0].links)
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
