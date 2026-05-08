package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.CoupleBirthData
import com.astrologyapi.sdk.models.PlanetName
import com.astrologyapi.sdk.models.ZodiacSign
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WesternNamespaceTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AstrologyAPI

    private val birth = BirthData(
        day = 10, month = 5, year = 1990,
        hour = 19, min = 55, lat = 19.20, lon = 72.83, tzone = 5.5,
    )
    private val couple = CoupleBirthData(
        person1 = birth,
        person2 = BirthData(day = 15, month = 8, year = 1992, hour = 10, min = 30,
            lat = 28.61, lon = 77.20, tzone = 5.5),
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

    // ── Core ──────────────────────────────────────────────────────────────────

    @Test fun `getPlanets posts to planets-tropical`() = runTest {
        ok()
        client.western.getPlanets(birth)
        assertTrue(server.takeRequest().path!!.contains("planets/tropical"))
    }

    @Test fun `getHouseCusps posts to house_cusps-tropical`() = runTest {
        ok()
        client.western.getHouseCusps(birth)
        assertTrue(server.takeRequest().path!!.contains("house_cusps/tropical"))
    }

    @Test fun `getHoroscope posts to western_horoscope`() = runTest {
        ok()
        client.western.getHoroscope(birth)
        assertTrue(server.takeRequest().path!!.contains("western_horoscope"))
    }

    @Test fun `getNatalWheelChart posts to natal_wheel_chart`() = runTest {
        ok()
        client.western.getNatalWheelChart(birth)
        assertTrue(server.takeRequest().path!!.contains("natal_wheel_chart"))
    }

    // ── House Reports ─────────────────────────────────────────────────────────

    @Test fun `getSignReport includes planet in path`() = runTest {
        ok()
        client.western.getSignReport(PlanetName.VENUS, birth)
        assertTrue(server.takeRequest().path!!.contains("general_sign_report/tropical/venus"))
    }

    @Test fun `getHouseReport includes planet in path`() = runTest {
        ok()
        client.western.getHouseReport(PlanetName.JUPITER, birth)
        assertTrue(server.takeRequest().path!!.contains("general_house_report/tropical/jupiter"))
    }

    @Test fun `getAscendantReport posts to general_ascendant_report-tropical`() = runTest {
        ok()
        client.western.getAscendantReport(birth)
        assertTrue(server.takeRequest().path!!.contains("general_ascendant_report/tropical"))
    }

    // ── Moon ──────────────────────────────────────────────────────────────────

    @Test fun `getMoonPhase posts to moon_phase_report`() = runTest {
        ok()
        client.western.getMoonPhase(birth)
        assertTrue(server.takeRequest().path!!.contains("moon_phase_report"))
    }

    @Test fun `getLunarMetrics posts to lunar_metrics`() = runTest {
        ok()
        client.western.getLunarMetrics(birth)
        assertTrue(server.takeRequest().path!!.contains("lunar_metrics"))
    }

    // ── Solar Return ──────────────────────────────────────────────────────────

    @Test fun `getSolarReturnDetails posts to solar_return_details`() = runTest {
        ok()
        client.western.getSolarReturnDetails(birth)
        assertTrue(server.takeRequest().path!!.contains("solar_return_details"))
    }

    // ── Personality ───────────────────────────────────────────────────────────

    @Test fun `getPersonality posts to personality_report-tropical`() = runTest {
        ok()
        client.western.getPersonality(birth)
        assertTrue(server.takeRequest().path!!.contains("personality_report/tropical"))
    }

    @Test fun `getRomanticPersonality posts to romantic_personality_report`() = runTest {
        ok()
        client.western.getRomanticPersonality(birth)
        assertTrue(server.takeRequest().path!!.contains("romantic_personality_report/tropical"))
    }

    @Test fun `getKarmaDestiny posts to karma_destiny_report-tropical`() = runTest {
        ok()
        client.western.getKarmaDestiny(couple)
        assertTrue(server.takeRequest().path!!.contains("karma_destiny_report/tropical"))
    }

    // ── Compatibility ─────────────────────────────────────────────────────────

    @Test fun `getZodiacCompatibility includes both signs in path`() = runTest {
        ok()
        client.western.getZodiacCompatibility(ZodiacSign.ARIES, ZodiacSign.LIBRA)
        assertTrue(server.takeRequest().path!!.contains("zodiac_compatibility/aries/libra"))
    }

    @Test fun `getSynastry flattens p_ and s_ prefixed keys`() = runTest {
        ok()
        client.western.getSynastry(couple)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("p_day"))
        assertTrue(body.contains("s_day"))
        assertTrue(body.contains("p_tzone"))
        assertTrue(body.contains("s_lat"))
    }

    @Test fun `getComposite posts to composite_horoscope`() = runTest {
        ok()
        client.western.getComposite(couple)
        assertTrue(server.takeRequest().path!!.contains("composite_horoscope"))
    }

    @Test fun `BirthData fields appear in request body`() = runTest {
        ok()
        client.western.getPlanets(birth)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("lat=19.2"))
        assertTrue(body.contains("tzone=5.5"))
    }

    @Test fun `getFriendship sends couple body`() = runTest {
        ok()
        client.western.getFriendship(couple)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("p_day=10"))
        assertTrue(body.contains("s_day=15"))
    }

    @Test fun `language is passed as Accept-Language header`() = runTest {
        ok()
        client.western.getPlanets(birth, "fr")
        assertEquals("fr", server.takeRequest().getHeader("Accept-Language"))
    }

    @Test fun `getPlanets preserves array responses`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""[{"name":"Sun"}]""")
        )
        val response = client.western.getPlanets(birth)
        assertTrue(response.isJsonArray)
    }
}
