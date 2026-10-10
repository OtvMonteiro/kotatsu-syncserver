package org.kotatsu.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kotatsu.model.favourite.FavouritesPackage
import org.kotatsu.plugins.currentUser
import org.kotatsu.plugins.toFutonSyncResponseJson
import org.kotatsu.resources.deltaFrom
import org.kotatsu.resources.setFavouritesSynchronized
import org.kotatsu.resources.syncFavourites

fun Route.favouriteRoutes() {
    authenticate("auth-jwt") {
        get("/resource/favourites") {
            val user = call.currentUser
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            val response = syncFavourites(user, null)
            call.respond(response.toFutonSyncResponseJson())
        }
        post<FavouritesPackage>("/resource/favourites") { request ->
            val user = call.currentUser
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            val response = withContext(Dispatchers.IO) {
                val result = syncFavourites(user, request)
                user.setFavouritesSynchronized(System.currentTimeMillis())
                result
            }

            val delta = response.deltaFrom(request)
            if (delta.favouriteCategories.isEmpty() && delta.favourites.isEmpty()) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(delta.toFutonSyncResponseJson())
            }
        }
    }
}
