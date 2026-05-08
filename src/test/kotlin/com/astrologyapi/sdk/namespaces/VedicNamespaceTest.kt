package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.models.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class VedicNamespaceTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AstrologyAPI

    private val birthData = BirthData(
        day = 10, month = 5, year = 1990,
        hour = 19, min = 55,
        lat = 19.20, lon = 72.83, tzone = 5.5,
    )

    private val matchData = MatchBirthData(
        male = birthData,
        female = BirthData(day=15, month=8, year=1992, hour=10, min=30, lat=28.61, lon=77.20, tzone=5.5),
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

    private fun enqueueOk() {
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("""{"ok":true}"""))
    }

    @Test fun `getBirthDetails posts to birth_details`() = runTest {
        enqueueOk()
        client.vedic.getBirthDetails(birthData)
        assertTrue(server.takeRequest().path!!.contains("birth_details"))
    }

    @Test fun `getPlanets posts to planets`() = runTest {
        enqueueOk()
        client.vedic.getPlanets(birthData)
        assertTrue(server.takeRequest().path!!.contains("/planets"))
    }

    @Test fun `getChart posts to horo_chart with chartId`() = runTest {
        enqueueOk()
        client.vedic.getChart(ChartId.D9, birthData)
        assertTrue(server.takeRequest().path!!.contains("horo_chart/D9"))
    }

    @Test fun `getPlanetAshtak posts to planet_ashtak with planet name`() = runTest {
        enqueueOk()
        client.vedic.getPlanetAshtak(PlanetName.SATURN, birthData)
        assertTrue(server.takeRequest().path!!.contains("planet_ashtak/saturn"))
    }

    @Test fun `getCurrentDasha posts to current_vdasha`() = runTest {
        enqueueOk()
        client.vedic.getCurrentDasha(birthData)
        assertTrue(server.takeRequest().path!!.contains("current_vdasha"))
    }

    @Test fun `getManglik posts to manglik`() = runTest {
        enqueueOk()
        client.vedic.getManglik(birthData)
        assertTrue(server.takeRequest().path!!.contains("manglik"))
    }

    @Test fun `getKalsarpaDosha posts to kalsarpa_details`() = runTest {
        enqueueOk()
        client.vedic.getKalsarpaDosha(birthData)
        assertTrue(server.takeRequest().path!!.contains("kalsarpa_details"))
    }

    @Test fun `getBasicPanchang posts to basic_panchang`() = runTest {
        enqueueOk()
        client.vedic.getBasicPanchang(birthData)
        assertTrue(server.takeRequest().path!!.contains("basic_panchang"))
    }

    @Test fun `getMatchReport flattens m_ and f_ keys`() = runTest {
        enqueueOk()
        client.vedic.getMatchReport(matchData)
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("m_day"))
        assertTrue(body.contains("f_day"))
        assertTrue(body.contains("m_tzone"))
        assertTrue(body.contains("f_lat"))
        assertTrue(body.contains("ayanamsha=LAHIRI"))
    }

    @Test fun `BirthData fields appear in request body`() = runTest {
        enqueueOk()
        client.vedic.getPlanets(birthData)
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("lat=19.2"))
        assertTrue(body.contains("tzone=5.5"))
        assertTrue(body.contains("ayanamsha=LAHIRI"))
    }

    @Test fun `language is passed as Accept-Language header`() = runTest {
        enqueueOk()
        client.vedic.getPlanets(birthData, "hi")
        assertEquals("hi", server.takeRequest().getHeader("Accept-Language"))
    }

    @Test fun `getAshtakootPoints posts to match_ashtakoot_points`() = runTest {
        enqueueOk()
        client.vedic.getAshtakootPoints(matchData)
        assertTrue(server.takeRequest().path!!.contains("match_ashtakoot_points"))
    }

    @Test fun `getVarshaphalDetails posts to varshaphal_details`() = runTest {
        enqueueOk()
        client.vedic.getVarshaphalDetails(birthData, yearCount = 34)
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("varshaphal_details"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("ayanamsha=LAHIRI"))
        assertTrue(body.contains("varshaphal_year=2024"))
    }

    @Test fun `getHouseReport posts to general_house_report with planet`() = runTest {
        enqueueOk()
        client.vedic.getHouseReport(PlanetName.JUPITER, birthData)
        assertTrue(server.takeRequest().path!!.contains("general_house_report/jupiter"))
    }

    @Test fun `getGemSuggestion posts to basic_gem_suggestion`() = runTest {
        enqueueOk()
        client.vedic.getGemSuggestion(birthData)
        assertTrue(server.takeRequest().path!!.contains("basic_gem_suggestion"))
    }

    @Test fun `getPlanets preserves array responses`() = runTest {
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("""[{"id":0}]"""))
        val response = client.vedic.getPlanets(birthData)
        assertTrue(response.isJsonArray)
    }

    @Test fun `getVarshaphalMuntha preserves scalar responses`() = runTest {
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("\"Virgo\""))
        val response = client.vedic.getVarshaphalMuntha(birthData, yearCount = 34)
        assertTrue(response.isJsonPrimitive)
        assertEquals("Virgo", response.asString)
    }
}
