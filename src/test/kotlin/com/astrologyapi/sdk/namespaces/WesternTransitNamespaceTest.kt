package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.models.BirthData
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class WesternTransitNamespaceTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AstrologyAPI

    private val birth = BirthData(
        day = 10, month = 5, year = 1990,
        hour = 19, min = 55, lat = 19.20, lon = 72.83, tzone = 5.5,
    )

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

    @Test fun `getDaily posts to tropical_transits-daily`() = runTest {
        ok()
        client.westernTransit.getDaily(birth)
        assertTrue(server.takeRequest().path!!.contains("tropical_transits/daily"))
    }

    @Test fun `getWeekly posts to tropical_transits-weekly`() = runTest {
        ok()
        client.westernTransit.getWeekly(birth)
        assertTrue(server.takeRequest().path!!.contains("tropical_transits/weekly"))
    }

    @Test fun `getMonthly posts to tropical_transits-monthly`() = runTest {
        ok()
        client.westernTransit.getMonthly(birth)
        assertTrue(server.takeRequest().path!!.contains("tropical_transits/monthly"))
    }

    @Test fun `getNatalDaily posts to natal_transits-daily`() = runTest {
        ok()
        client.westernTransit.getNatalDaily(birth)
        assertTrue(server.takeRequest().path!!.contains("natal_transits/daily"))
    }

    @Test fun `getNatalWeekly posts to natal_transits-weekly`() = runTest {
        ok()
        client.westernTransit.getNatalWeekly(birth)
        assertTrue(server.takeRequest().path!!.contains("natal_transits/weekly"))
    }

    @Test fun `BirthData fields appear in request body`() = runTest {
        ok()
        client.westernTransit.getDaily(birth)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("tzone=5.5"))
    }
}
