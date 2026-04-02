package app.verdant.filter

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Provider
@ApplicationScoped
class RateLimitFilter : ContainerRequestFilter {

    private data class RateWindow(val count: AtomicInteger = AtomicInteger(0), var windowStart: Long = System.currentTimeMillis())

    private val requestCounts = ConcurrentHashMap<String, RateWindow>()
    private val windowMs = 60_000L // 1 minute window
    private val maxRequests = 120 // 120 requests per minute per IP

    override fun filter(context: ContainerRequestContext) {
        val ip = context.getHeaderString("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: context.getHeaderString("X-Real-IP")
            ?: "unknown"

        val window = requestCounts.computeIfAbsent(ip) { RateWindow() }
        val now = System.currentTimeMillis()

        synchronized(window) {
            if (now - window.windowStart > windowMs) {
                window.count.set(0)
                window.windowStart = now
            }
        }

        if (window.count.incrementAndGet() > maxRequests) {
            context.abortWith(
                Response.status(429)
                    .entity(mapOf("message" to "Rate limit exceeded. Try again later."))
                    .build()
            )
        }
    }
}
