package com.astrologyapi.sdk.http

import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.errors.*
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class HttpClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: HttpClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = HttpClient(
            AstrologyAPIConfig(
                userId = "test_user",
                apiKey = "test_key",
                baseJsonUrl = server.url("/").toString(),
                basePdfUrl = server.url("/").toString(),
            )
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueJson(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }

    private fun enqueuePdf(bytes: ByteArray = byteArrayOf(0x25, 0x50, 0x44, 0x46)) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/pdf")
                .setBody(okio.Buffer().write(bytes))
        )
    }

    // ── URL Construction ──────────────────────────────────────────────────────

    @Test fun `postJson builds correct URL with version`() = runTest {
        enqueueJson("""{"ok":true}""")
        client.postJson("planets", emptyMap())
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("/v1/planets"), "Path was: ${request.path}")
    }

    // ── Headers ───────────────────────────────────────────────────────────────

    @Test fun `postJson uses Basic auth when apiKey is not ak-prefixed`() = runTest {
        enqueueJson("""{"ok":true}""")
        client.postJson("test", emptyMap())
        val request = server.takeRequest()
        assertTrue(request.getHeader("Authorization")!!.startsWith("Basic "))
        assertNull(request.getHeader("x-astrologyapi-key"))
    }

    @Test fun `postJson uses x-astrologyapi-key header when apiKey is ak-prefixed`() = runTest {
        val headerClient = HttpClient(
            AstrologyAPIConfig(
                apiKey = "ak-test_key",
                baseJsonUrl = server.url("/").toString(),
                basePdfUrl = server.url("/").toString(),
            )
        )
        enqueueJson("""{"ok":true}""")
        headerClient.postJson("test", emptyMap())
        val request = server.takeRequest()
        assertEquals("ak-test_key", request.getHeader("x-astrologyapi-key"))
        assertNull(request.getHeader("Authorization"))
    }

    @Test fun `postJson sets User-Agent header`() = runTest {
        enqueueJson("""{"ok":true}""")
        client.postJson("test", emptyMap())
        val request = server.takeRequest()
        assertTrue(request.getHeader("User-Agent")!!.startsWith("astrologyapi-kotlin/"))
    }

    @Test fun `postJson sets Content-Type header`() = runTest {
        enqueueJson("""{"ok":true}""")
        client.postJson("test", emptyMap())
        val request = server.takeRequest()
        assertTrue(request.getHeader("Content-Type")!!.contains("application/json"))
    }

    @Test fun `postJson sends Accept-Language when language provided`() = runTest {
        enqueueJson("""{"ok":true}""")
        client.postJson("test", emptyMap(), "hi")
        val request = server.takeRequest()
        assertEquals("hi", request.getHeader("Accept-Language"))
    }

    @Test fun `postJson omits Accept-Language when language is null`() = runTest {
        enqueueJson("""{"ok":true}""")
        client.postJson("test", emptyMap(), null)
        val request = server.takeRequest()
        assertNull(request.getHeader("Accept-Language"))
    }

    // ── Body Serialization ────────────────────────────────────────────────────

    @Test fun `postJson serializes body as JSON`() = runTest {
        enqueueJson("""{"ok":true}""")
        client.postJson("test", mapOf("day" to 10, "month" to 5))
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains(""""day":10"""))
        assertTrue(body.contains(""""month":5"""))
    }

    @Test fun `postForm serializes body as form-urlencoded`() = runTest {
        enqueueJson("""{"ok":true}""")
        client.postForm("test", mapOf("day" to 10, "name" to "Aarav"))
        val request = server.takeRequest()
        assertTrue(request.getHeader("Content-Type")!!.contains("application/x-www-form-urlencoded"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("name=Aarav"))
    }

    // ── Response Parsing ──────────────────────────────────────────────────────

    @Test fun `postJson returns JsonObject on success`() = runTest {
        enqueueJson("""{"planets":[{"name":"Sun"}]}""")
        val result = client.postJson("test", emptyMap())
        assertTrue(result.has("planets"))
        assertTrue(result["planets"].isJsonArray)
    }

    @Test fun `postElement preserves scalar JSON responses`() = runTest {
        enqueueJson(""""scalar"""")
        val result = client.postElement("test", emptyMap(), encoding = RequestEncoding.JSON)
        assertEquals(JsonPrimitive("scalar"), result)
    }

    @Test fun `postPdf returns ByteArray on success`() = runTest {
        enqueuePdf()
        val result = client.postPdf("test_pdf", emptyMap())
        assertTrue(result.isNotEmpty())
    }

    // ── Error Mapping ─────────────────────────────────────────────────────────

    @Test fun `400 throws ValidationException`() = runTest {
        enqueueJson("""{"message":"invalid day"}""", 400)
        val ex = assertFailsWith<ValidationException> {
            client.postJson("test", emptyMap())
        }
        assertTrue(ex.message!!.contains("invalid day"))
    }

    @Test fun `401 throws AuthenticationException`() = runTest {
        enqueueJson("""{"message":"bad key"}""", 401)
        val ex = assertFailsWith<AuthenticationException> {
            client.postJson("test", emptyMap())
        }
        assertEquals(401, ex.status)
    }

    @Test fun `402 throws QuotaExceededException`() = runTest {
        enqueueJson("""{"message":"quota exceeded"}""", 402)
        assertFailsWith<QuotaExceededException> {
            client.postJson("test", emptyMap())
        }
    }

    @Test fun `403 throws PlanRestrictedException`() = runTest {
        enqueueJson("""{"message":"plan restricted"}""", 403)
        assertFailsWith<PlanRestrictedException> {
            client.postJson("test", emptyMap())
        }
    }

    @Test fun `429 throws RateLimitException`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(429)
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "30")
                .setBody("""{"message":"slow down"}""")
        )
        val ex = assertFailsWith<RateLimitException> {
            client.postJson("test", emptyMap())
        }
        assertEquals(30, ex.retryAfter)
    }

    @Test fun `500 throws ServerException`() = runTest {
        enqueueJson("""{"message":"internal error"}""", 500)
        val ex = assertFailsWith<ServerException> {
            client.postJson("test", emptyMap())
        }
        assertEquals(500, ex.status)
    }

    @Test fun `503 throws ServerException`() = runTest {
        enqueueJson("""{}""", 503)
        val ex = assertFailsWith<ServerException> {
            client.postJson("test", emptyMap())
        }
        assertEquals(503, ex.status)
    }

    @Test fun `API message is extracted from error body`() = runTest {
        enqueueJson("""{"message":"Your key is wrong"}""", 401)
        val ex = assertFailsWith<AuthenticationException> {
            client.postJson("test", emptyMap())
        }
        assertTrue(ex.message!!.contains("Your key is wrong"))
    }

    @Test fun `API error field also extracted`() = runTest {
        enqueueJson("""{"error":"Something went wrong"}""", 400)
        val ex = assertFailsWith<ValidationException> {
            client.postJson("test", emptyMap())
        }
        assertTrue(ex.message!!.contains("Something went wrong"))
    }

    @Test fun `nested array errors are normalized into a readable message`() = runTest {
        enqueueJson("""{"errors":[["Missing gender"],["Missing ayanamsha"]]}""", 400)
        val ex = assertFailsWith<ValidationException> {
            client.postJson("test", emptyMap())
        }
        assertTrue(ex.message!!.contains("Missing gender"))
        assertTrue(ex.message!!.contains("Missing ayanamsha"))
    }
}
