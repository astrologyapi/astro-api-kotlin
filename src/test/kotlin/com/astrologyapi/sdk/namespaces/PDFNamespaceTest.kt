package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.CoupleBirthData
import com.astrologyapi.sdk.models.MatchBirthData
import com.astrologyapi.sdk.models.PDFBranding
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class PDFNamespaceTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AstrologyAPI

    private val birth = BirthData(
        day = 10, month = 5, year = 1990,
        hour = 19, min = 55, lat = 19.20, lon = 72.83, tzone = 5.5,
        name = "Aarav Mehta", place = "Mumbai, Maharashtra, India", gender = "male",
    )
    private val matchData = MatchBirthData(
        male = birth,
        female = BirthData(day = 15, month = 8, year = 1992, hour = 10, min = 30,
            lat = 28.61, lon = 77.20, tzone = 5.5, name = "Kavya Sharma", place = "Delhi, India", gender = "female"),
    )
    private val coupleData = CoupleBirthData(person1 = birth, person2 = matchData.female)

    @BeforeEach fun setUp() {
        server = MockWebServer()
        server.start()
        client = AstrologyAPI(AstrologyAPIConfig(
            userId = "u", apiKey = "k",
            basePdfUrl = server.url("/").toString(),
        ))
    }

    @AfterEach fun tearDown() { server.shutdown() }

    private val fakePdf = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF

    private fun okPdf() = server.enqueue(
        MockResponse().setResponseCode(200)
            .addHeader("Content-Type", "application/pdf")
            .setBody(okio.Buffer().write(fakePdf))
    )

    // ── Vedic PDF ─────────────────────────────────────────────────────────────

    @Test fun `getMiniKundli posts to mini_horoscope_pdf`() = runTest {
        okPdf()
        client.pdf.vedic.getMiniKundli(birth)
        assertTrue(server.takeRequest().path!!.contains("mini_horoscope_pdf"))
    }

    @Test fun `getBasicHoroscope posts to basic_horoscope_pdf`() = runTest {
        okPdf()
        client.pdf.vedic.getBasicHoroscope(birth)
        assertTrue(server.takeRequest().path!!.contains("basic_horoscope_pdf"))
    }

    @Test fun `getProfessionalHoroscope posts to pro_horoscope_pdf`() = runTest {
        okPdf()
        client.pdf.vedic.getProfessionalHoroscope(birth)
        assertTrue(server.takeRequest().path!!.contains("pro_horoscope_pdf"))
    }

    @Test fun `getMatchMaking uses the PDF field contract`() = runTest {
        okPdf()
        client.pdf.vedic.getMatchMaking(matchData)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("m_first_name=Aarav"))
        assertTrue(body.contains("m_minute=55"))
        assertTrue(body.contains("f_first_name=Kavya"))
        assertTrue(body.contains("f_timezone=5.5"))
        assertTrue(body.contains("dashakoot=true"))
    }

    @Test fun `vedic branding fields appear in body`() = runTest {
        okPdf()
        val branding = PDFBranding(companyName = "AstroTest", logoUrl = "https://example.com/logo.png")
        client.pdf.vedic.getMiniKundli(birth, name = "Arjun", branding = branding)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("company_name=AstroTest"))
        assertTrue(body.contains("gender=male"))
        assertTrue(body.contains("place=Mumbai"))
    }

    @Test fun `getMiniKundli returns ByteArray PDF bytes`() = runTest {
        okPdf()
        val result = client.pdf.vedic.getMiniKundli(birth)
        assertContentEquals(fakePdf, result)
    }

    // ── Western PDF ───────────────────────────────────────────────────────────

    @Test fun `getNatalChart posts to natal_horoscope_report-tropical`() = runTest {
        okPdf()
        client.pdf.western.getNatalChart(birth)
        assertTrue(server.takeRequest().path!!.contains("natal_horoscope_report/tropical"))
    }

    @Test fun `getLifeForecast posts to life_forecast_report-tropical`() = runTest {
        okPdf()
        client.pdf.western.getLifeForecast(birth)
        assertTrue(server.takeRequest().path!!.contains("life_forecast_report/tropical"))
    }

    @Test fun `getSolarReturn posts to solar_return_report-tropical`() = runTest {
        okPdf()
        client.pdf.western.getSolarReturn(birth)
        assertTrue(server.takeRequest().path!!.contains("solar_return_report/tropical"))
    }

    @Test fun `getSynastry uses the PDF couple field contract`() = runTest {
        okPdf()
        client.pdf.western.getSynastry(coupleData)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("p_first_name=Aarav"))
        assertTrue(body.contains("p_minute=55"))
        assertTrue(body.contains("s_first_name=Kavya"))
        assertTrue(body.contains("s_timezone=5.5"))
    }

    @Test fun `western branding fields appear in body`() = runTest {
        okPdf()
        val branding = PDFBranding(companyName = "AstroWest", chartStyle = "south-indian")
        client.pdf.western.getNatalChart(birth, branding = branding)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("company_name=AstroWest"))
        assertTrue(body.contains("minute=55"))
    }
}
