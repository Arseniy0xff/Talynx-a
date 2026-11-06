package com.example.talynxa

import com.example.talynxa.data.model.MatchMode
import com.example.talynxa.data.model.NoteFile
import com.example.talynxa.data.model.NoteItem
import com.example.talynxa.data.repository.NoteRepository
import com.example.talynxa.ui.NoteViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteViewModelTest {

    private lateinit var viewModel: NoteViewModel

    @Before
    fun setUp() {
        viewModel = NoteViewModel(NoteRepository())
    }

    @Test
    fun testAddItemAndTagStatistics() {
        viewModel.addItem("First item", listOf("work", "urgent"))
        viewModel.addItem("Second item", listOf("work", "personal"))

        val currentContent = viewModel.uiState.value.currentNote.content
        assertTrue(currentContent.any { it.text == "First item" })
        assertTrue(currentContent.any { it.text == "Second item" })

        val tagCounts = NoteViewModel.calculateTagCounts(currentContent)
        assertEquals(2, tagCounts["work"])
        assertEquals(1, tagCounts["urgent"])
        assertEquals(1, tagCounts["personal"])
    }

    @Test
    fun testEditAndDeleteItem() {
        viewModel.addItem("Initial Text", listOf("tag1"))
        val addedItem = viewModel.uiState.value.currentNote.content.last()

        viewModel.editItem(addedItem.id, "Updated Text", listOf("tag1", "tag2"))
        val editedItem = viewModel.uiState.value.currentNote.content.find { it.id == addedItem.id }
        assertEquals("Updated Text", editedItem?.text)
        assertEquals(listOf("tag1", "tag2"), editedItem?.tags)

        viewModel.deleteItem(addedItem.id)
        val deletedItem = viewModel.uiState.value.currentNote.content.find { it.id == addedItem.id }
        assertEquals(null, deletedItem)
    }

    @Test
    fun testAddAndRemoveTagFromItem() {
        viewModel.addItem("Sample item", listOf("tag1"))
        val item = viewModel.uiState.value.currentNote.content.last()

        viewModel.addTagToItem(item.id, "tag2")
        var updatedItem = viewModel.uiState.value.currentNote.content.find { it.id == item.id }
        assertEquals(listOf("tag1", "tag2"), updatedItem?.tags)

        viewModel.removeTagFromItem(item.id, "tag1")
        updatedItem = viewModel.uiState.value.currentNote.content.find { it.id == item.id }
        assertEquals(listOf("tag2"), updatedItem?.tags)
    }

    @Test
    fun testMoveItemReordering() {
        viewModel.updateNoteDetails("Test Note", "test.json")
        // Clear content and add 3 items
        val initialNote = NoteFile(
            name = "Test",
            fileName = "test.json",
            content = listOf(
                NoteItem(text = "Item 0"),
                NoteItem(text = "Item 1"),
                NoteItem(text = "Item 2")
            )
        )
        viewModel.loadFromJsonString(NoteRepository().encodeJsonString(initialNote))

        viewModel.moveItem(0, 2)
        val content = viewModel.uiState.value.currentNote.content
        assertEquals("Item 1", content[0].text)
        assertEquals("Item 2", content[1].text)
        assertEquals("Item 0", content[2].text)
    }

    @Test
    fun testTagFilterMatchModeAllVsAny() {
        val items = listOf(
            NoteItem(text = "Item A", tags = listOf("android", "kotlin")),
            NoteItem(text = "Item B", tags = listOf("android", "java")),
            NoteItem(text = "Item C", tags = listOf("kotlin", "swift"))
        )

        val selectedTags = setOf("android", "kotlin")

        // MatchMode.ALL: item must contain BOTH android AND kotlin
        val allMatches = NoteViewModel.filterItems(items, "", selectedTags, MatchMode.ALL)
        assertEquals(1, allMatches.size)
        assertEquals("Item A", allMatches[0].text)

        // MatchMode.ANY: item contains AT LEAST ONE of android OR kotlin
        val anyMatches = NoteViewModel.filterItems(items, "", selectedTags, MatchMode.ANY)
        assertEquals(3, anyMatches.size)
    }

    @Test
    fun testSearchQueryFiltering() {
        val items = listOf(
            NoteItem(text = "Buy groceries", tags = listOf("home")),
            NoteItem(text = "Write unit tests", tags = listOf("code")),
            NoteItem(text = "Read book", tags = listOf("leisure"))
        )

        val searchResults = NoteViewModel.filterItems(items, "unit", emptySet(), MatchMode.ALL)
        assertEquals(1, searchResults.size)
        assertEquals("Write unit tests", searchResults[0].text)

        val tagSearchResults = NoteViewModel.filterItems(items, "home", emptySet(), MatchMode.ALL)
        assertEquals(1, tagSearchResults.size)
        assertEquals("Buy groceries", tagSearchResults[0].text)
    }
}
