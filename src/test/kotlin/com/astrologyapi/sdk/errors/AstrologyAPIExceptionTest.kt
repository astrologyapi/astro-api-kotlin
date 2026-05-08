package com.astrologyapi.sdk.errors

import org.junit.jupiter.api.Test
import kotlin.test.*

class AstrologyAPIExceptionTest {

    @Test fun `AuthenticationException is subclass of AstrologyAPIException`() {
        assertTrue(AuthenticationException() is AstrologyAPIException)
    }

    @Test fun `ValidationException is subclass of AstrologyAPIException`() {
        assertTrue(ValidationException("bad field") is AstrologyAPIException)
    }

    @Test fun `QuotaExceededException is subclass of AstrologyAPIException`() {
        assertTrue(QuotaExceededException() is AstrologyAPIException)
    }

    @Test fun `PlanRestrictedException is subclass of AstrologyAPIException`() {
        assertTrue(PlanRestrictedException() is AstrologyAPIException)
    }

    @Test fun `RateLimitException is subclass of AstrologyAPIException`() {
        assertTrue(RateLimitException() is AstrologyAPIException)
    }

    @Test fun `ServerException is subclass of AstrologyAPIException`() {
        assertTrue(ServerException("err", 500) is AstrologyAPIException)
    }

    @Test fun `NetworkException is subclass of AstrologyAPIException`() {
        assertTrue(NetworkException("err", RuntimeException()) is AstrologyAPIException)
    }

    @Test fun `AuthenticationException has status 401`() {
        assertEquals(401, AuthenticationException().status)
    }

    @Test fun `QuotaExceededException has status 402`() {
        assertEquals(402, QuotaExceededException().status)
    }

    @Test fun `PlanRestrictedException has status 403`() {
        assertEquals(403, PlanRestrictedException().status)
    }

    @Test fun `RateLimitException has status 429`() {
        assertEquals(429, RateLimitException().status)
    }

    @Test fun `ServerException stores status code`() {
        assertEquals(503, ServerException("err", 503).status)
    }

    @Test fun `NetworkException has null status`() {
        assertNull(NetworkException("err", RuntimeException()).status)
    }

    @Test fun `RateLimitException stores retryAfter`() {
        val ex = RateLimitException(retryAfter = 60)
        assertEquals(60, ex.retryAfter)
    }

    @Test fun `ValidationException stores field`() {
        val ex = ValidationException("bad", field = "day")
        assertEquals("day", ex.field)
    }

    @Test fun `AstrologyAPIException stores body`() {
        val ex = AstrologyAPIException("error", 400, body = """{"message":"test"}""")
        assertEquals("""{"message":"test"}""", ex.body)
    }

    @Test fun `exception messages are set correctly`() {
        assertEquals("custom message", AuthenticationException("custom message").message)
        assertEquals("custom message", QuotaExceededException("custom message").message)
    }

    @Test fun `NetworkException wraps cause`() {
        val cause = IOException("connect failed")
        val ex = NetworkException("network err", cause)
        assertSame(cause, ex.cause)
    }
}

private class IOException(message: String) : java.io.IOException(message)
