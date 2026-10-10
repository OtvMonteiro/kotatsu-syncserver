package org.kotatsu.resources

import org.kotatsu.model.favourite.FavouritesPackage
import org.kotatsu.model.history.HistoryPackage

/** Returns only merged favorite data not already present in the client's upload. */
fun FavouritesPackage.deltaFrom(request: FavouritesPackage): FavouritesPackage {
    val requestedCategories = request.favouriteCategories.toHashSet()
    val requestedFavourites = request.favourites.toHashSet()
    return copy(
        favouriteCategories = favouriteCategories.filterNot(requestedCategories::contains),
        favourites = favourites.filterNot(requestedFavourites::contains),
    )
}

/** Returns only merged history entries not already present in the client's upload. */
fun HistoryPackage.deltaFrom(request: HistoryPackage): HistoryPackage {
    val requestedHistory = request.history.toHashSet()
    return copy(history = history.filterNot(requestedHistory::contains))
}
