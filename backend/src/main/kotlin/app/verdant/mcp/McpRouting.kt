package app.verdant.mcp

import io.vertx.ext.web.Router
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

@ApplicationScoped
class McpRouting(private val config: McpConfig) {
    fun init(@Observes router: Router) {
        router.route("/mcp").order(-10000).handler { ctx ->
            ctx.addHeadersEndHandler {
                ctx.response().putHeader("Cache-Control", "no-store")
                if (ctx.response().statusCode == 401) ctx.response().putHeader("WWW-Authenticate", config.challenge)
            }
            if (!config.acceptsOrigin(ctx.request().getHeader("Origin"))) ctx.response().setStatusCode(403).end()
            else ctx.next()
        }
    }
}
