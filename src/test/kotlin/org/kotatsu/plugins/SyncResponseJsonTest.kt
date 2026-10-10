package org.kotatsu.plugins

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.kotatsu.model.favourite.Favourite
import org.kotatsu.model.favourite.FavouritesPackage
import org.kotatsu.model.manga.Manga
import org.kotatsu.model.manga.MangaTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncResponseJsonTest {

    @Test
    fun omits_futon_unsupported_fields_from_sync_responses() {
        val tag = MangaTag(
            id = 1L,
            title = "Genre",
            key = "genre",
            source = "source",
            pinned = true,
        )
        val manga = Manga(
            id = 1L,
            title = "Manga",
            altTitle = null,
            url = "/manga",
            publicUrl = "https://example.test/manga",
            rating = 0.5f,
            coverUrl = "https://example.test/cover.jpg",
            largeCoverUrl = null,
            tags = setOf(tag),
            state = null,
            author = null,
            source = "source",
            nsfw = true,
        )

        val favourite = Favourite(
            mangaId = 1L,
            manga = manga,
            categoryId = 1,
            sortKey = 0,
            pinned = true,
            createdAt = 1L,
            deletedAt = 0L,
        )
        val response = FavouritesPackage(
            favouriteCategories = emptyList(),
            favourites = listOf(favourite),
            timestamp = 1L,
        )
        val responseJson = response.toFutonSyncResponseJson().jsonObject
        val favouriteJson = responseJson.getValue("favourites").jsonArray.single().jsonObject
        val mangaJson = favouriteJson.getValue("manga").jsonObject
        val tagJson = mangaJson.getValue("tags").jsonArray.single().jsonObject

        assertFalse(mangaJson.containsKey("nsfw"))
        assertFalse(tagJson.containsKey("pinned"))
        assertTrue(favouriteJson.getValue("pinned").jsonPrimitive.content.toBoolean())
        assertEquals("Manga", mangaJson.getValue("title").jsonPrimitive.content)
        assertTrue(manga.nsfw)
        assertTrue(tag.pinned)
        assertTrue(favourite.pinned)
    }
}
