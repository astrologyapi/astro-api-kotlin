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

class ChineseNamespaceTest {

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

    @Test fun `getZodiac posts to chinese_zodiac`() = runTest {
        ok()
        client.chinese.getZodiac(birth)
        assertTrue(server.takeRequest().path!!.contains("chinese_zodiac"))
    }

    @Test fun `getYearForecast posts to chinese_year_forecast`() = runTest {
        ok()
        client.chinese.getYearForecast(birth)
        assertTrue(server.takeRequest().path!!.contains("chinese_year_forecast"))
    }

    @Test fun `BirthData fields appear in request body`() = runTest {
        ok()
        client.chinese.getZodiac(birth)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("year=1990"))
        assertTrue(body.contains("month=5"))
    }
}
