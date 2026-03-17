package app.verdant.resource

import io.vertx.ext.web.Router
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

@ApplicationScoped
class SpaRouting {

    fun init(@Observes router: Router) {
        router.get().handler { ctx ->
            val path = ctx.normalizedPath()
            when {
                // Let API calls, static files (with extensions), and Quarkus paths pass through
                path.startsWith("/api/") || path.startsWith("/q/") || path.contains(".") -> ctx.next()
                // Admin SPA
                path.startsWith("/admin") -> ctx.reroute("/admin/index.html")
                // Main web SPA
                else -> ctx.reroute("/index.html")
            }
        }
    }
}
