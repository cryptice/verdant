package app.verdant.mcp

import app.verdant.dto.ProductionTargetResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class McpPlanningTest {
    @Test fun `weekly coverage respects species date and final partial week`() {
        val start = LocalDate.of(2027, 7, 1)
        val target = ProductionTargetResponse(1, 1, 10, "Dahlia", 100, start, start.plusDays(9), null, Instant.now())
        val rows = McpTools.weeklyCoverage(target, listOf(PlannedStems(10, start, 60), PlannedStems(10, start.plusDays(6), 10), PlannedStems(20, start, 1000), PlannedStems(10, start.minusDays(1), 1000), PlannedStems(10, start.plusDays(9), 120), PlannedStems(10, start.plusDays(10), 1000)))
        assertEquals(2, rows.size)
        assertEquals(70L, rows[0]["plannedStems"])
        assertEquals(30L, rows[0]["shortfallStems"])
        assertEquals(120L, rows[1]["plannedStems"])
        assertEquals(0L, rows[1]["shortfallStems"])
    }
    @Test fun `redirects require HTTPS or local loopback without fragments or credentials`() {
        for (uri in listOf("https://chatgpt.com/connector/oauth/callback", "http://localhost:8123/callback", "http://127.0.0.1:8123/callback", "http://[::1]:8123/callback")) assertTrue(McpOAuthService.validRedirect(uri), uri)
        for (uri in listOf("http://attacker.test/callback", "https://user:password@example.test/", "https://example.test/#token", "javascript:alert(1)", "//example.test/")) assertFalse(McpOAuthService.validRedirect(uri), uri)
    }
    @Test fun `PKCE RFC7636 example and exact resource binding`() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", McpSecrets.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        val config = McpConfig("https://verdantplanner.com", "https://verdantplanner.com")
        assertTrue(config.acceptsResource("https://verdantplanner.com/mcp"))
        for (value in listOf("https://verdantplanner.com/mcp?x=1", "https://verdantplanner.com/mcp/", "https://attacker.test/mcp", "http://verdantplanner.com/mcp", "https://user@verdantplanner.com/mcp")) assertFalse(config.acceptsResource(value))
    }
}
