package com.example.talynxa.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.booleanOrNull

object FlexibleIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("FlexibleId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.intOrNull != null -> element.intOrNull.toString()
                    element.longOrNull != null -> element.longOrNull.toString()
                    element.doubleOrNull != null -> element.doubleOrNull.toString()
                    element.booleanOrNull != null -> element.booleanOrNull.toString()
                    else -> element.contentOrNull ?: "0"
                }
            }
            else -> element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        val intVal = value.toIntOrNull()
        if (intVal != null) {
            encoder.encodeInt(intVal)
        } else {
            encoder.encodeString(value)
        }
    }
}

object FlexibleLinksSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("FlexibleLinks")

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder ?: return emptyList()
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is kotlinx.serialization.json.JsonArray -> {
                element.mapNotNull { item ->
                    if (item is JsonPrimitive) {
                        when {
                            item.isString -> item.content
                            item.intOrNull != null -> item.intOrNull.toString()
                            item.longOrNull != null -> item.longOrNull.toString()
                            item.doubleOrNull != null -> item.doubleOrNull.toString()
                            item.booleanOrNull != null -> item.booleanOrNull.toString()
                            else -> item.contentOrNull
                        }
                    } else {
                        item.toString()
                    }
                }
            }
            element is JsonPrimitive -> {
                val str = when {
                    element.isString -> element.content
                    element.intOrNull != null -> element.intOrNull.toString()
                    else -> element.contentOrNull
                }
                if (str != null) listOf(str) else emptyList()
            }
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        val jsonEncoder = encoder as? kotlinx.serialization.json.JsonEncoder
        val jsonArray = kotlinx.serialization.json.JsonArray(
            value.map { v ->
                val intVal = v.toIntOrNull()
                if (intVal != null) {
                    JsonPrimitive(intVal)
                } else {
                    JsonPrimitive(v)
                }
            }
        )
        jsonEncoder?.encodeJsonElement(jsonArray) ?: encoder.encodeString(jsonArray.toString())
    }
}

@Serializable
data class NoteItem(
    @Serializable(with = FlexibleIdSerializer::class)
    @SerialName("id") val id: String = "0",
    @SerialName("text") val text: String = "",
    @SerialName("tags") val tags: List<String> = emptyList(),
    @Serializable(with = FlexibleLinksSerializer::class)
    @SerialName("links") val links: List<String> = emptyList()
)

@Serializable
data class NoteFile(
    @SerialName("name") val name: String = "",
    @SerialName("content") val content: List<NoteItem> = emptyList()
)
