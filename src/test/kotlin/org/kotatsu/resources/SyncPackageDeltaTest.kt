package org.kotatsu.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import org.kotatsu.model.category.Category
import org.kotatsu.model.favourite.Favourite
import org.kotatsu.model.favourite.FavouritesPackage
import org.kotatsu.model.history.History
import org.kotatsu.model.history.HistoryPackage
import org.kotatsu.model.manga.Manga

class SyncPackageDeltaTest {

    @Test
    fun favourites_delta_contains_only_categories_and_favourites_absent_or_changed_in_request() {
        val localCategory = category(1, "Reading")
        val remoteCategory = category(2, "Later")
        val localFavourite = favourite(1L, "Local", categoryId = 1)
        val remoteFavourite = favourite(2L, "Remote", categoryId = 2)
        val changedFavourite = localFavourite.copy(sortKey = 42)
        val request = FavouritesPackage(
            favouriteCategories = listOf(localCategory),
            favourites = listOf(localFavourite),
            timestamp = 1L,
        )
        val merged = FavouritesPackage(
            favouriteCategories = listOf(remoteCategory, localCategory),
            favourites = listOf(remoteFavourite, changedFavourite),
            timestamp = 2L,
        )

        val delta = merged.deltaFrom(request)

        assertEquals(listOf(remoteCategory), delta.favouriteCategories)
        assertEquals(listOf(remoteFavourite, changedFavourite), delta.favourites)
        assertEquals(2L, delta.timestamp)
    }

    @Test
    fun history_delta_omits_records_already_present_even_when_response_order_differs() {
        val localHistory = history(1L, updatedAt = 100L)
        val remoteHistory = history(2L, updatedAt = 200L)
        val newerHistory = localHistory.copy(updatedAt = 300L, page = 7)
        val request = HistoryPackage(listOf(localHistory, remoteHistory), timestamp = 1L)
        val merged = HistoryPackage(listOf(remoteHistory, localHistory, newerHistory), timestamp = 2L)

        val delta = merged.deltaFrom(request)

        assertEquals(listOf(newerHistory), delta.history)
        assertEquals(2L, delta.timestamp)
    }

    @Test
    fun identical_favourites_in_a_different_order_produce_an_empty_delta() {
        val firstCategory = category(1, "Reading")
        val secondCategory = category(2, "Later")
        val firstFavourite = favourite(1L, "First", categoryId = 1)
        val secondFavourite = favourite(2L, "Second", categoryId = 2)
        val request = FavouritesPackage(
            favouriteCategories = listOf(firstCategory, secondCategory),
            favourites = listOf(firstFavourite, secondFavourite),
            timestamp = 1L,
        )
        val merged = request.copy(
            favouriteCategories = listOf(secondCategory, firstCategory),
            favourites = listOf(secondFavourite, firstFavourite),
            timestamp = 2L,
        )

        val delta = merged.deltaFrom(request)

        assertEquals(emptyList(), delta.favouriteCategories)
        assertEquals(emptyList(), delta.favourites)
    }

    private fun category(id: Int, title: String) = Category(
        id = id,
        createdAt = 1L,
        sortKey = id,
        track = false,
        title = title,
        order = "updated",
        deletedAt = 0L,
        showInLib = true,
    )

    private fun favourite(mangaId: Long, title: String, categoryId: Int) = Favourite(
        mangaId = mangaId,
        manga = manga(mangaId, title),
        categoryId = categoryId,
        sortKey = 0,
        pinned = false,
        createdAt = 1L,
        deletedAt = 0L,
    )

    private fun history(mangaId: Long, updatedAt: Long) = History(
        mangaId = mangaId,
        manga = manga(mangaId, "Manga $mangaId"),
        createdAt = 1L,
        updatedAt = updatedAt,
        chapterId = 1L,
        page = 1,
        scroll = 0f,
        percent = 0f,
        chapters = 1,
        deletedAt = 0L,
    )

    private fun manga(id: Long, title: String) = Manga(
        id = id,
        title = title,
        altTitle = null,
        url = "/manga/$id",
        publicUrl = "https://example.test/manga/$id",
        rating = 0.5f,
        coverUrl = "https://example.test/cover/$id.jpg",
        largeCoverUrl = null,
        tags = emptySet(),
        state = null,
        author = null,
        source = "source",
    )
}
