package com.astrologyapi.sdk.models

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelsTest {

    private val birth = BirthData(
        day = 10, month = 5, year = 1990,
        hour = 19, min = 55, lat = 19.20, lon = 72.83, tzone = 5.5,
    )

    // ── BirthData ─────────────────────────────────────────────────────────────

    @Test fun `BirthData toMap contains all 8 fields`() {
        val map = birth.toMap()
        assertEquals(8, map.size)
        assertEquals(10, map["day"])
        assertEquals(5, map["month"])
        assertEquals(1990, map["year"])
        assertEquals(19, map["hour"])
        assertEquals(55, map["min"])
        assertEquals(19.20, map["lat"])
        assertEquals(72.83, map["lon"])
        assertEquals(5.5, map["tzone"])
    }

    // ── MatchBirthData ────────────────────────────────────────────────────────

    @Test fun `MatchBirthData flatten uses m_ and f_ prefixes`() {
        val female = BirthData(day = 15, month = 8, year = 1992, hour = 10, min = 30,
            lat = 28.61, lon = 77.20, tzone = 5.5)
        val map = MatchBirthData(male = birth, female = female).flatten()

        assertEquals(16, map.size)
        assertEquals(10, map["m_day"])
        assertEquals(5, map["m_month"])
        assertEquals(19.20, map["m_lat"])
        assertEquals(15, map["f_day"])
        assertEquals(8, map["f_month"])
        assertEquals(28.61, map["f_lat"])
    }

    // ── CoupleBirthData ───────────────────────────────────────────────────────

    @Test fun `CoupleBirthData flatten uses p_ and s_ prefixes`() {
        val person2 = BirthData(day = 15, month = 8, year = 1992, hour = 10, min = 30,
            lat = 28.61, lon = 77.20, tzone = 5.5)
        val map = CoupleBirthData(person1 = birth, person2 = person2).flatten()

        assertEquals(16, map.size)
        assertEquals(10, map["p_day"])
        assertEquals(19.20, map["p_lat"])
        assertEquals(15, map["s_day"])
        assertEquals(28.61, map["s_lat"])
    }

    // ── NumerologyData ────────────────────────────────────────────────────────

    @Test fun `NumerologyData toMap contains all 4 fields`() {
        val map = NumerologyData(day = 10, month = 5, year = 1990, name = "Arjun").toMap()
        assertEquals(4, map.size)
        assertEquals(10, map["day"])
        assertEquals("Arjun", map["name"])
    }

    // ── PDFBranding ───────────────────────────────────────────────────────────

    @Test fun `PDFBranding toMap omits null fields`() {
        val map = PDFBranding(companyName = "AstroTest").toMap()
        assertEquals(1, map.size)
        assertEquals("AstroTest", map["company_name"])
        assertFalse(map.containsKey("logo_url"))
    }

    @Test fun `PDFBranding toMap uses snake_case keys`() {
        val branding = PDFBranding(
            logoUrl = "https://example.com/logo.png",
            companyName = "AstroTest",
            chartStyle = "south-indian",
        )
        val map = branding.toMap()
        assertTrue(map.containsKey("logo_url"))
        assertTrue(map.containsKey("company_name"))
        assertTrue(map.containsKey("chart_style"))
        assertEquals("south-indian", map["chart_style"])
    }

    @Test fun `PDFBranding empty branding produces empty map`() {
        assertEquals(0, PDFBranding().toMap().size)
    }

    // ── Enums ─────────────────────────────────────────────────────────────────

    @Test fun `ChartId toString returns id string`() {
        assertEquals("D9", ChartId.D9.toString())
        assertEquals("D1", ChartId.D1.toString())
        assertEquals("D60", ChartId.D60.toString())
    }

    @Test fun `ZodiacSign toString returns lowercase sign`() {
        assertEquals("aries", ZodiacSign.ARIES.toString())
        assertEquals("scorpio", ZodiacSign.SCORPIO.toString())
        assertEquals("pisces", ZodiacSign.PISCES.toString())
    }

    @Test fun `PlanetName toString returns lowercase planet`() {
        assertEquals("sun", PlanetName.SUN.toString())
        assertEquals("saturn", PlanetName.SATURN.toString())
        assertEquals("ascendant", PlanetName.ASCENDANT.toString())
    }
}
