package app.verdant.filter

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger
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
        // Cloud Run appends the address it received the request from to
        // X-Forwarded-For. Earlier entries are whatever the client sent, so
        // keying on the first one let anyone pick a fresh key per request.
        val ip = context.getHeaderString("X-Forwarded-For")?.split(",")?.lastOrNull()?.trim()?.takeIf { it.isNotEmpty() }
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

        val count = window.count.incrementAndGet()
        if (count > maxRequests) {
            // Once per window, masked: enough to see which client keys hit the limit.
            if (count == maxRequests + 1) log.warnf("Rate limit exceeded for %s", maskForLog(ip))
            context.abortWith(
                Response.status(429)
                    .entity(mapOf("message" to "Rate limit exceeded. Try again later."))
                    .build()
            )
        }
    }

    companion object {
        private val log = Logger.getLogger(RateLimitFilter::class.java)

        fun maskForLog(ip: String): String = when {
            ip.count { it == '.' } == 3 -> ip.substringBeforeLast('.') + ".x"
            ip.contains(':') -> ip.split(':').take(4).joinToString(":") + "::x"
            else -> ip
        }
    }
}
