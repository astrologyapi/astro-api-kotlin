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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KPNamespaceTest {

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

    @Test fun `getPlanets posts to kp_planets`() = runTest {
        ok()
        client.kp.getPlanets(birth)
        assertTrue(server.takeRequest().path!!.contains("kp_planets"))
    }

    @Test fun `getHouseCusps posts to kp_house_cusps`() = runTest {
        ok()
        client.kp.getHouseCusps(birth)
        assertTrue(server.takeRequest().path!!.contains("kp_house_cusps"))
    }

    @Test fun `getBirthChart posts to kp_birth_chart`() = runTest {
        ok()
        client.kp.getBirthChart(birth)
        assertTrue(server.takeRequest().path!!.contains("kp_birth_chart"))
    }

    @Test fun `getHouseSignificator posts to kp_house_significator`() = runTest {
        ok()
        client.kp.getHouseSignificator(birth)
        assertTrue(server.takeRequest().path!!.contains("kp_house_significator"))
    }

    @Test fun `getPlanetSignificator posts to kp_planet_significator`() = runTest {
        ok()
        client.kp.getPlanetSignificator(birth)
        assertTrue(server.takeRequest().path!!.contains("kp_planet_significator"))
    }

    @Test fun `getHoroscope posts to kp_horoscope`() = runTest {
        ok()
        client.kp.getHoroscope(birth)
        assertTrue(server.takeRequest().path!!.contains("kp_horoscope"))
    }

    @Test fun `BirthData fields appear in request body`() = runTest {
        ok()
        client.kp.getPlanets(birth)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("tzone=5.5"))
        assertTrue(body.contains("ayanamsha=LAHIRI"))
    }

    @Test fun `getHoroscope includes aspects and ayanamsha defaults`() = runTest {
        ok()
        client.kp.getHoroscope(birth)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("aspects=true"))
        assertTrue(body.contains("ayanamsha=LAHIRI"))
    }

    @Test fun `getPlanets preserves array responses`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""[{"planet_id":0}]""")
        )
        val response = client.kp.getPlanets(birth)
        assertTrue(response.isJsonArray)
    }

    @Test fun `language is passed as Accept-Language header`() = runTest {
        ok()
        client.kp.getPlanets(birth, "hi")
        assertEquals("hi", server.takeRequest().getHeader("Accept-Language"))
    }
}
