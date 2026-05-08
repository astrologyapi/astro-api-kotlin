package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.NumerologyData
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class NumerologyNamespaceTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AstrologyAPI

    private val numData = NumerologyData(day = 10, month = 5, year = 1990, name = "Arjun Kumar")
    private val birth = BirthData(
        day = 10, month = 5, year = 1990,
        hour = 19, min = 55, lat = 19.20, lon = 72.83, tzone = 5.5,
        name = "Arjun Kumar",
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

    // ── Vedic ─────────────────────────────────────────────────────────────────

    @Test fun `getTable posts to numero_table`() = runTest {
        ok()
        client.numerology.getTable(numData)
        assertTrue(server.takeRequest().path!!.contains("numero_table"))
    }

    @Test fun `getReport posts to numero_report`() = runTest {
        ok()
        client.numerology.getReport(numData)
        assertTrue(server.takeRequest().path!!.contains("numero_report"))
    }

    @Test fun `getFavTime posts to numero_fav_time`() = runTest {
        ok()
        client.numerology.getFavTime(numData)
        assertTrue(server.takeRequest().path!!.contains("numero_fav_time"))
    }

    @Test fun `getPlaceVastu posts to numero_place_vastu`() = runTest {
        ok()
        client.numerology.getPlaceVastu(numData)
        assertTrue(server.takeRequest().path!!.contains("numero_place_vastu"))
    }

    @Test fun `getFastsReport posts to numero_fasts_report`() = runTest {
        ok()
        client.numerology.getFastsReport(numData)
        assertTrue(server.takeRequest().path!!.contains("numero_fasts_report"))
    }

    @Test fun `getFavLord posts to numero_fav_lord`() = runTest {
        ok()
        client.numerology.getFavLord(numData)
        assertTrue(server.takeRequest().path!!.contains("numero_fav_lord"))
    }

    @Test fun `getFavMantra posts to numero_fav_mantra`() = runTest {
        ok()
        client.numerology.getFavMantra(numData)
        assertTrue(server.takeRequest().path!!.contains("numero_fav_mantra"))
    }

    @Test fun `getDailyPrediction posts to numero_prediction_daily`() = runTest {
        ok()
        client.numerology.getDailyPrediction(numData)
        assertTrue(server.takeRequest().path!!.contains("numero_prediction/daily"))
    }

    // ── Western ───────────────────────────────────────────────────────────────

    @Test fun `getNumerologicalNumbers posts to numerological_numbers`() = runTest {
        ok()
        client.numerology.getNumerologicalNumbers(numData)
        assertTrue(server.takeRequest().path!!.contains("numerological_numbers"))
    }

    @Test fun `getLifepathNumber posts to lifepath_number`() = runTest {
        ok()
        client.numerology.getLifepathNumber(numData)
        assertTrue(server.takeRequest().path!!.contains("lifepath_number"))
    }

    @Test fun `getPersonalityNumber posts to personality_number`() = runTest {
        ok()
        client.numerology.getPersonalityNumber(numData)
        assertTrue(server.takeRequest().path!!.contains("personality_number"))
    }

    @Test fun `getExpressionNumber posts to expression_number`() = runTest {
        ok()
        client.numerology.getExpressionNumber(numData)
        assertTrue(server.takeRequest().path!!.contains("expression_number"))
    }

    @Test fun `getSoulUrgeNumber posts to soul_urge_number`() = runTest {
        ok()
        client.numerology.getSoulUrgeNumber(numData)
        assertTrue(server.takeRequest().path!!.contains("soul_urge_number"))
    }

    @Test fun `getChallengeNumbers posts to challenge_numbers`() = runTest {
        ok()
        client.numerology.getChallengeNumbers(numData)
        assertTrue(server.takeRequest().path!!.contains("challenge_numbers"))
    }

    @Test fun `getPersonalDay posts to personal_day_prediction`() = runTest {
        ok()
        client.numerology.getPersonalDay(birth)
        assertTrue(server.takeRequest().path!!.contains("personal_day_prediction"))
    }

    @Test fun `getPersonalMonth posts to personal_month_prediction`() = runTest {
        ok()
        client.numerology.getPersonalMonth(birth)
        assertTrue(server.takeRequest().path!!.contains("personal_month_prediction"))
    }

    @Test fun `getPersonalYear posts to personal_year_prediction`() = runTest {
        ok()
        client.numerology.getPersonalYear(birth)
        assertTrue(server.takeRequest().path!!.contains("personal_year_prediction"))
    }

    @Test fun `NumerologyData fields appear in request body`() = runTest {
        ok()
        client.numerology.getTable(numData)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("name=Arjun"))
        assertTrue(body.contains("year=1990"))
    }

    @Test fun `western numerology uses live full_name plus date fields`() = runTest {
        ok()
        client.numerology.getNumerologicalNumbers(numData)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("full_name=Arjun%20Kumar"))
        assertTrue(body.contains("date=10"))
        assertFalse(body.contains("&name="))
        assertFalse(body.startsWith("name="))
    }

    @Test fun `personal day sends only numerology fields from birth data`() = runTest {
        ok()
        client.numerology.getPersonalDay(birth)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("month=5"))
        assertTrue(body.contains("year=1990"))
        assertTrue(body.contains("date=10"))
        assertTrue(body.contains("full_name=Arjun%20Kumar"))
        assertFalse(body.contains("hour="))
        assertFalse(body.contains("lat="))
        assertFalse(body.contains("lon="))
        assertFalse(body.contains("tzone="))
    }
}
