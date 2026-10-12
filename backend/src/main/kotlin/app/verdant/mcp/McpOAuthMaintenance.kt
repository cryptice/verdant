package app.verdant.mcp

import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class McpOAuthMaintenance(private val repo: McpOAuthRepository) {
    @Scheduled(every = "1h", delayed = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    fun cleanExpired() = repo.cleanExpired()
}
