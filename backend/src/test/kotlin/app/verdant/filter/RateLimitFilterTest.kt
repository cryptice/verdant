package app.verdant.filter

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.ConcurrentHashMap

class RateLimitFilterTest {

    private lateinit var filter: RateLimitFilter

    @BeforeEach
    fun setUp() {
        filter = RateLimitFilter()
    }

    private fun makeContext(ip: String, useXForwardedFor: Boolean = true): ContainerRequestContext {
        val context: ContainerRequestContext = mock()
        if (useXForwardedFor) {
            whenever(context.getHeaderString("X-Forwarded-For")).thenReturn(ip)
            whenever(context.getHeaderString("X-Real-IP")).thenReturn(null)
        } else {
            whenever(context.getHeaderString("X-Forwarded-For")).thenReturn(null)
            whenever(context.getHeaderString("X-Real-IP")).thenReturn(ip)
        }
        return context
    }

    @Test
    fun `allows requests under limit`() {
        repeat(10) {
            val context = makeContext("1.2.3.4")
            filter.filter(context)
            verify(context, never()).abortWith(any())
        }
    }

    @Test
    fun `blocks requests over limit`() {
        // Send exactly 120 requests (the limit) — all should pass
        repeat(120) {
            val context = makeContext("5.5.5.5")
            filter.filter(context)
            verify(context, never()).abortWith(any())
        }

        // The 121st request should be blocked
        val context = makeContext("5.5.5.5")
        filter.filter(context)
        verify(context).abortWith(argThat { status == 429 })
    }

    @Test
    fun `different IPs have separate limits`() {
        // Exhaust the limit for IP A
        repeat(121) {
            filter.filter(makeContext("10.0.0.1"))
        }

        // IP B should still be allowed
        val contextB = makeContext("10.0.0.2")
        filter.filter(contextB)
        verify(contextB, never()).abortWith(any())
    }

    @Test
    fun `window resets after time`() {
        // Exhaust the limit
        repeat(121) {
            filter.filter(makeContext("2.2.2.2"))
        }

        // Reach into the filter and backdate the window start so it appears expired
        val requestCountsField = RateLimitFilter::class.java.getDeclaredField("requestCounts")
        requestCountsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val requestCounts = requestCountsField.get(filter) as ConcurrentHashMap<*, *>

        val windowObj = requestCounts["2.2.2.2"]!!
        val windowStartField = windowObj.javaClass.getDeclaredField("windowStart")
        windowStartField.isAccessible = true
        // Set windowStart to 2 minutes ago so the window is considered expired
        windowStartField.set(windowObj, System.currentTimeMillis() - 120_000L)

        // Next request should be allowed (window resets)
        val context = makeContext("2.2.2.2")
        filter.filter(context)
        verify(context, never()).abortWith(any())
    }

    @Test
    fun `keys on the address the platform appended, not one the client supplied`() {
        // Cloud Run appends the real client address; anything before it came from the client.
        repeat(120) { i ->
            val c = makeContext("198.51.100.$i, 203.0.113.9")
            filter.filter(c)
            verify(c, never()).abortWith(any())
        }
        val blocked = makeContext("192.0.2.77, 203.0.113.9")
        filter.filter(blocked)
        verify(blocked).abortWith(argThat { status == 429 })
    }

    @Test
    fun `masks the last part of an address for logging`() {
        assertEquals("203.0.113.x", RateLimitFilter.maskForLog("203.0.113.9"))
        assertEquals("2001:db8:1:2::x", RateLimitFilter.maskForLog("2001:db8:1:2:3:4:5:6"))
        assertEquals("unknown", RateLimitFilter.maskForLog("unknown"))
    }

    @Test
    fun `uses X-Forwarded-For header`() {
        val context: ContainerRequestContext = mock()
        whenever(context.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4")
        whenever(context.getHeaderString("X-Real-IP")).thenReturn("9.9.9.9")

        filter.filter(context)
        verify(context, never()).abortWith(any())

        // Exhaust limit using X-Forwarded-For IP — X-Real-IP should remain unaffected
        repeat(120) {
            val c = mock<ContainerRequestContext>()
            whenever(c.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4")
            whenever(c.getHeaderString("X-Real-IP")).thenReturn("9.9.9.9")
            filter.filter(c)
        }

        // 1.2.3.4 is now over limit
        val blockedCtx = mock<ContainerRequestContext>()
        whenever(blockedCtx.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4")
        whenever(blockedCtx.getHeaderString("X-Real-IP")).thenReturn("9.9.9.9")
        filter.filter(blockedCtx)
        verify(blockedCtx).abortWith(argThat { status == 429 })

        // 9.9.9.9 as X-Real-IP (no X-Forwarded-For) is unaffected
        val unaffectedCtx = mock<ContainerRequestContext>()
        whenever(unaffectedCtx.getHeaderString("X-Forwarded-For")).thenReturn(null)
        whenever(unaffectedCtx.getHeaderString("X-Real-IP")).thenReturn("9.9.9.9")
        filter.filter(unaffectedCtx)
        verify(unaffectedCtx, never()).abortWith(any())
    }
}
