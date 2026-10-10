package org.kotatsu.plugins

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

@PublishedApi
internal val syncResponseJson = Json { encodeDefaults = true }

/** Serializes a sync response while hiding fields unsupported by Futon's strict sync DTOs. */
internal inline fun <reified T> T.toFutonSyncResponseJson(): JsonElement =
    syncResponseJson.encodeToJsonElement(this).withoutFutonUnsupportedMangaFields()

@PublishedApi
internal fun JsonElement.withoutFutonUnsupportedMangaFields(): JsonElement = when (this) {
    is JsonObject -> JsonObject(
        mapValues { (key, value) ->
            if (key == "manga" && value is JsonObject) {
                value.withoutFutonUnsupportedMangaProperties()
            } else {
                value.withoutFutonUnsupportedMangaFields()
            }
        },
    )

    is JsonArray -> JsonArray(map(JsonElement::withoutFutonUnsupportedMangaFields))
    else -> this
}

@PublishedApi
internal fun JsonObject.withoutFutonUnsupportedMangaProperties(): JsonObject = JsonObject(
    filterKeys { it != "nsfw" }
        .mapValues { (key, value) ->
            if (key == "tags" && value is JsonArray) {
                JsonArray(value.map { tag ->
                    if (tag is JsonObject) JsonObject(tag.filterKeys { it != "pinned" }) else tag
                })
            } else {
                value
            }
        },
)
