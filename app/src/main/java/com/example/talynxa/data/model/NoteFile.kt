package com.example.talynxa.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
data class NoteItem(
    @SerialName("text") val text: String = "",
    @SerialName("tags") val tags: List<String> = emptyList(),
    @Transient val id: String = UUID.randomUUID().toString()
)

@Serializable
data class NoteFile(
    @SerialName("name") val name: String = "",
    @SerialName("file_name") val fileName: String = "",
    @SerialName("content") val content: List<NoteItem> = emptyList()
)
