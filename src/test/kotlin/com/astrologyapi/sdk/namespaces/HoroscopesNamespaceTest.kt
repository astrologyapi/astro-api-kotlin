package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.ZodiacSign
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HoroscopesNamespaceTest {

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

    @Test fun `getDaily includes zodiac sign in path`() = runTest {
        ok()
        client.horoscopes.getDaily(ZodiacSign.ARIES)
        assertTrue(server.takeRequest().path!!.contains("sun_sign_prediction/daily/aries"))
    }

    @Test fun `getNext includes zodiac sign in path`() = runTest {
        ok()
        client.horoscopes.getNext(ZodiacSign.SCORPIO)
        assertTrue(server.takeRequest().path!!.contains("sun_sign_prediction/daily/next/scorpio"))
    }

    @Test fun `getPrevious includes zodiac sign in path`() = runTest {
        ok()
        client.horoscopes.getPrevious(ZodiacSign.CAPRICORN)
        assertTrue(server.takeRequest().path!!.contains("sun_sign_prediction/daily/previous/capricorn"))
    }

    @Test fun `getDailyConsolidated includes zodiac sign in path`() = runTest {
        ok()
        client.horoscopes.getDailyConsolidated(ZodiacSign.LEO)
        assertTrue(server.takeRequest().path!!.contains("sun_sign_consolidated/daily/leo"))
    }

    @Test fun `getMonthly includes zodiac sign in path`() = runTest {
        ok()
        client.horoscopes.getMonthly(ZodiacSign.TAURUS)
        assertTrue(server.takeRequest().path!!.contains("horoscope_prediction/monthly/taurus"))
    }

    @Test fun `getDailyNakshatra posts to daily_nakshatra_prediction`() = runTest {
        ok()
        client.horoscopes.getDailyNakshatra(birth)
        assertTrue(server.takeRequest().path!!.contains("daily_nakshatra_prediction"))
    }

    @Test fun `sun-sign endpoints send empty body`() = runTest {
        ok()
        client.horoscopes.getDaily(ZodiacSign.GEMINI)
        assertEquals("", server.takeRequest().body.readUtf8())
    }

    @Test fun `language header is passed for sign endpoints`() = runTest {
        ok()
        client.horoscopes.getDaily(ZodiacSign.VIRGO, "hi")
        assertEquals("hi", server.takeRequest().getHeader("Accept-Language"))
    }
}
