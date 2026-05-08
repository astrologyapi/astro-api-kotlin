package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class LocationNamespaceTest {

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

    @Test fun `getGeoDetails posts to geo_details`() = runTest {
        ok()
        client.location.getGeoDetails("Mumbai")
        assertTrue(server.takeRequest().path!!.contains("geo_details"))
    }

    @Test fun `getGeoDetails includes place and maxRows in body`() = runTest {
        ok()
        client.location.getGeoDetails("Delhi", maxRows = 10)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("place=Delhi"))
        assertTrue(body.contains("maxRows=10"))
    }

    @Test fun `getGeoDetails defaults to maxRows 6`() = runTest {
        ok()
        client.location.getGeoDetails("Chennai")
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("maxRows=6"))
    }

    @Test fun `getTimezone posts to timezone_with_dst`() = runTest {
        ok()
        client.location.getTimezone(10, 5, 1990, 19, 55, 19.20, 72.83)
        assertTrue(server.takeRequest().path!!.contains("timezone_with_dst"))
    }

    @Test fun `getTimezone includes all coordinate fields in body`() = runTest {
        ok()
        client.location.getTimezone(10, 5, 1990, 19, 55, 19.20, 72.83)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("lat=19.2"))
        assertTrue(body.contains("lon=72.83"))
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("year=1990"))
    }
}
