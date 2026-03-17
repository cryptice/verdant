package app.verdant.resource

import io.vertx.ext.web.Router
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

@ApplicationScoped
class SpaRouting {

    fun init(@Observes router: Router) {
        router.get().handler { ctx ->
            val path = ctx.normalizedPath()
            // Let API calls, static files (with extensions), and Quarkus paths pass through
            if (path.startsWith("/api/") || path.startsWith("/q/") || path.contains(".")) {
                ctx.next()
            } else {
                ctx.reroute("/index.html")
            }
        }
    }
}
