package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TarotNamespaceTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AstrologyAPI

    @BeforeEach fun setUp() {
        server = MockWebServer()
        server.start()
        client = AstrologyAPI(AstrologyAPIConfig(
            userId = "u", apiKey = "k",
            baseJsonUrl = server.url("/").toString(),
        ))
    }

    @AfterEach fun tearDown() { server.shutdown() }

    private fun ok() = server.enqueue(
        MockResponse().setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("""{"ok":true}""")
    )

    @Test fun `getPredictions posts to tarot_predictions`() = runTest {
        ok()
        client.tarot.getPredictions()
        assertTrue(server.takeRequest().path!!.contains("tarot_predictions"))
    }

    @Test fun `getYesNo posts to yes_no_tarot`() = runTest {
        ok()
        client.tarot.getYesNo()
        assertTrue(server.takeRequest().path!!.contains("yes_no_tarot"))
    }

    @Test fun `tarot endpoints send required scalar params`() = runTest {
        ok()
        client.tarot.getPredictions()
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("love=12"))
        assertTrue(body.contains("career=23"))
        assertTrue(body.contains("finance=45"))
    }

    @Test fun `yes-no tarot sends tarot_id`() = runTest {
        ok()
        client.tarot.getYesNo()
        val body = server.takeRequest().body.readUtf8()
        assertEquals("tarot_id=5", body)
    }

    @Test fun `language is passed as Accept-Language header`() = runTest {
        ok()
        client.tarot.getPredictions("hi")
        assertEquals("hi", server.takeRequest().getHeader("Accept-Language"))
    }
}
