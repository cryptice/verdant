package app.verdant.android.data

import java.io.IOException

sealed class AppError(override val message: String) : IOException(message) {
    class Network(message: String = "No internet connection. Check your network and try again.") : AppError(message)
    class Unauthorized(message: String = "Your session has expired. Please sign in again.") : AppError(message)
    class NotFound(message: String = "The requested item was not found.") : AppError(message)
    class Server(message: String = "Something went wrong. Please try again later.") : AppError(message)
    class Unknown(message: String = "An unexpected error occurred.") : AppError(message)
}
