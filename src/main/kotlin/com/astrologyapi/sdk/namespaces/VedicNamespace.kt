package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.ChartId
import com.astrologyapi.sdk.models.MatchBirthData
import com.astrologyapi.sdk.models.PlanetName
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Vedic astrology namespace — Parashari & Jaimini systems.
 *
 * Covers birth charts, divisional charts (D1–D60), all Dasha systems
 * (Vimshottari, Char, Yogini), Panchang, Muhurta, Matchmaking,
 * Varshaphal, Doshas, Remedies, and interpretive reports.
 */
class VedicNamespace internal constructor(private val http: HttpClient) {

    private suspend fun post(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postForm(endpoint, body, language)

    private suspend fun postElement(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postElement(endpoint, body, language)

    private suspend fun postJsonBody(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postJson(endpoint, body, language)

    // ── Birth Data ─────────────────────────────────────────────────────────────

    /** Core birth chart summary: ascendant, sun sign, moon sign, nakshatra. */
    suspend fun getBirthDetails(data: BirthData, language: String? = null): JsonObject =
        post("birth_details", data.ayanamshaBody(), language)

    /** Extended astrological details including yoga, karana, and tithi. */
    suspend fun getAstroDetails(data: BirthData, language: String? = null): JsonObject =
        post("astro_details", data.ayanamshaBody(), language)

    /** Ayanamsha value for the given birth date and time. */
    suspend fun getAyanamsha(data: BirthData, language: String? = null): JsonElement =
        postElement("ayanamsha", data.toMap(), language)

    // ── Planets ────────────────────────────────────────────────────────────────

    /** Planetary positions with sign, nakshatra, house, and degree. */
    suspend fun getPlanets(data: BirthData, language: String? = null): JsonElement =
        postElement("planets", data.ayanamshaBody(), language)

    /** Extended planetary data including retrograde status, speed, and dignity. */
    suspend fun getExtendedPlanets(data: BirthData, language: String? = null): JsonElement =
        postElement("planets/extended", data.ayanamshaBody(), language)

    /** Ashtak Varga table for a specific planet. */
    suspend fun getPlanetAshtak(planet: PlanetName, data: BirthData, language: String? = null): JsonObject =
        post("planet_ashtak/$planet", data.ayanamshaBody(), language)

    /** Sarvashtakavarga (combined Ashtak Varga) table. */
    suspend fun getSarvashtak(data: BirthData, language: String? = null): JsonObject =
        post("sarvashtak", data.ayanamshaBody(), language)

    /** Ghat Chakra calculation. */
    suspend fun getGhatChakra(data: BirthData, language: String? = null): JsonObject =
        post("ghat_chakra", data.ayanamshaBody(), language)

    // ── Horoscope Charts ───────────────────────────────────────────────────────

    /**
     * Divisional chart data (D1 = Lagna, D9 = Navamsa, D10 = Dashamsa, etc.).
     * @param chartId One of D1, D2, D3, D4, D5, D7, D9, D10, D12, D16, D20, D24, D27, D30, D40, D45, D60
     */
    suspend fun getChart(chartId: ChartId, data: BirthData, language: String? = null): JsonElement =
        postElement("horo_chart/$chartId", data.ayanamshaBody(), language)

    /** SVG/image representation of a divisional chart. */
    suspend fun getChartImage(chartId: ChartId, data: BirthData, language: String? = null): JsonObject =
        post("horo_chart_image/$chartId", data.ayanamshaBody(), language)

    /** Extended Lagna chart with additional dignity and aspect data. */
    suspend fun getExtendedChart(
        data: BirthData,
        chartId: ChartId = ChartId.D1,
        language: String? = null,
    ): JsonElement = postElement("horo_chart_extended/$chartId", data.ayanamshaBody(), language)

    // ── Vimshottari Dasha ──────────────────────────────────────────────────────

    /** Current running Vimshottari Maha Dasha, Antardasha, and Pratyantardasha. */
    suspend fun getCurrentDasha(data: BirthData, language: String? = null): JsonObject =
        post("current_vdasha", data.ayanamshaBody(), language)

    /** Full Vimshottari Maha Dasha sequence (all 120-year cycle). */
    suspend fun getMajorDasha(data: BirthData, language: String? = null): JsonElement =
        postElement("major_vdasha", data.ayanamshaBody(), language)

    /** Sub-dashas (Antardasha) within the current Maha Dasha. */
    suspend fun getSubDasha(md: String, data: BirthData, language: String? = null): JsonElement =
        postElement("sub_vdasha/$md", data.ayanamshaBody(), language)

    /** Sub-sub-dashas (Pratyantardasha) for the current period. */
    suspend fun getSubSubDasha(md: String, ad: String, data: BirthData, language: String? = null): JsonElement =
        postElement("sub_sub_vdasha/$md/$ad", data.ayanamshaBody(), language)

    /** Sub-sub-sub-dashas (Sookshma) for the current period. */
    suspend fun getSubSubSubDasha(
        md: String,
        ad: String,
        pd: String,
        data: BirthData,
        language: String? = null,
    ): JsonElement = postElement("sub_sub_sub_vdasha/$md/$ad/$pd", data.ayanamshaBody(), language)

    /** Sub-sub-sub-sub-dashas (Prana) for the current period. */
    suspend fun getSubSubSubSubDasha(
        md: String,
        ad: String,
        pd: String,
        sd: String,
        data: BirthData,
        language: String? = null,
    ): JsonElement = postElement("sub_sub_sub_sub_vdasha/$md/$ad/$pd/$sd", data.ayanamshaBody(), language)

    // ── Char Dasha (Jaimini) ───────────────────────────────────────────────────

    /** Current running Char Dasha and Antardasha. */
    suspend fun getCurrentCharDasha(data: BirthData, language: String? = null): JsonObject =
        post("current_chardasha", data.ayanamshaBody(), language)

    /** All Char Maha Dashas in sequence. */
    suspend fun getMajorCharDasha(data: BirthData, language: String? = null): JsonElement =
        postElement("major_chardasha", data.ayanamshaBody(), language)

    /** Sub-dashas within a specific Char Maha Dasha sign. */
    suspend fun getSubCharDasha(mahaDasha: String, data: BirthData, language: String? = null): JsonObject =
        post("sub_chardasha/$mahaDasha", data.ayanamshaBody(), language)

    // ── Yogini Dasha ───────────────────────────────────────────────────────────

    /** Current running Yogini Dasha and sub-period. */
    suspend fun getCurrentYoginiDasha(data: BirthData, language: String? = null): JsonObject =
        post("current_yogini_dasha", data.ayanamshaBody(), language)

    // ── Doshas ─────────────────────────────────────────────────────────────────

    /** Kalsarpa Dosha analysis — type, strength, and remedies. */
    suspend fun getKalsarpaDosha(data: BirthData, language: String? = null): JsonObject =
        post("kalsarpa_details", data.ayanamshaBody(), language)

    /** Whether Mangal (Mars) Dosha is present and its effects. */
    suspend fun getManglik(data: BirthData, language: String? = null): JsonObject =
        post("manglik", data.toMap(), language)

    /** Current Sade Sati phase status (rising, peak, or setting). */
    suspend fun getSadhesatiStatus(data: BirthData, language: String? = null): JsonObject =
        post("sadhesati_current_status", data.ayanamshaBody(), language)

    /** Full Sade Sati life timeline — all past and future occurrences. */
    suspend fun getSadhesatiLifeDetails(data: BirthData, language: String? = null): JsonElement =
        postElement("sadhesati_life_details", data.ayanamshaBody(), language)

    /** Pitra Dosha analysis and significance. */
    suspend fun getPitraDosha(data: BirthData, language: String? = null): JsonObject =
        post("pitra_dosha_report", data.ayanamshaBody(), language)

    // ── Remedies ───────────────────────────────────────────────────────────────

    /** Recommended gemstones based on the birth chart. */
    suspend fun getGemSuggestion(data: BirthData, language: String? = null): JsonObject =
        post("basic_gem_suggestion", data.ayanamshaBody(), language)

    /** Suggested pujas and rituals for planetary improvement. */
    suspend fun getPujaSuggestion(data: BirthData, language: String? = null): JsonObject =
        post("puja_suggestion", data.ayanamshaBody(), language)

    /** Rudraksha bead recommendations based on the birth chart. */
    suspend fun getRudrakshaSuggestion(data: BirthData, language: String? = null): JsonObject =
        post("rudraksha_suggestion", data.ayanamshaBody(), language)

    /** Remedies to mitigate the effects of Sade Sati. */
    suspend fun getSadhesatiRemedies(data: BirthData, language: String? = null): JsonObject =
        post("sadhesati_remedies", data.toMap(), language)

    // ── Panchang ───────────────────────────────────────────────────────────────

    /** Basic Panchang: tithi, nakshatra, yoga, karana, vara. */
    suspend fun getBasicPanchang(data: BirthData, language: String? = null): JsonObject =
        post("basic_panchang", data.toMap(), language)

    /** Advanced Panchang including additional timings and planetary positions. */
    suspend fun getAdvancedPanchang(data: BirthData, language: String? = null): JsonObject =
        post("advanced_panchang", data.toMap(), language)

    /** Planetary positions at the time of Panchang calculation. */
    suspend fun getPlanetPanchang(data: BirthData, language: String? = null): JsonElement =
        postElement("planet_panchang", data.toMap(), language)

    /** Tamil Panchang for South Indian traditions. */
    suspend fun getTamilPanchang(data: BirthData, language: String? = null): JsonObject =
        post("tamil_panchang", data.toMap(), language)

    /** Hindu festivals and auspicious days for the given year/location. */
    suspend fun getFestival(data: BirthData, language: String? = null): JsonObject =
        post("panchang_festival", data.toMap(), language)

    // ── Muhurta ────────────────────────────────────────────────────────────────

    /** Hora Muhurta — planetary hour timings for the day. */
    suspend fun getHoraMuhurta(data: BirthData, language: String? = null): JsonObject =
        post("hora_muhurta", data.toMap(), language)

    /** Chaughadiya Muhurta — auspicious and inauspicious time blocks. */
    suspend fun getChaughadiyaMuhurta(data: BirthData, language: String? = null): JsonObject =
        post("chaughadiya_muhurta", data.toMap(), language)

    // ── Matchmaking ────────────────────────────────────────────────────────────

    /** Basic birth details comparison for two individuals. */
    suspend fun getMatchBirthDetails(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_birth_details", data.ayanamshaBody(), language)

    /** Compatibility obstructions (Dosha analysis for marriage matching). */
    suspend fun getMatchObstructions(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_obstructions", data.ayanamshaBody(), language)

    /** Astrological details for both individuals side by side. */
    suspend fun getMatchAstroDetails(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_astro_details", data.ayanamshaBody(), language)

    /** Planetary positions for both individuals. */
    suspend fun getMatchPlanetDetails(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_planet_details", data.ayanamshaBody(), language)

    /** Manglik Dosha compatibility report for both individuals. */
    suspend fun getMatchManglikReport(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_manglik_report", data.ayanamshaBody(), language)

    /** Ashtakoot Guna Milan points (maximum 36). */
    suspend fun getAshtakootPoints(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_ashtakoot_points", data.ayanamshaBody(), language)

    /** Dashakoot compatibility points. */
    suspend fun getDashakootPoints(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_dashakoot_points", data.ayanamshaBody(), language)

    /** Overall compatibility percentage. */
    suspend fun getMatchPercentage(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_percentage", data.ayanamshaBody(), language)

    /** Consolidated matchmaking summary report. */
    suspend fun getMatchReport(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_making_report", data.ayanamshaBody(), language)

    /** Comprehensive matchmaking report with detailed analysis. */
    suspend fun getDetailedMatchReport(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_making_detailed_report", data.ayanamshaBody(), language)

    /** Simplified matchmaking report for quick compatibility overview. */
    suspend fun getSimpleMatchReport(data: MatchBirthData, language: String? = null): JsonObject =
        post("match_simple_report", data.flatten(), language)

    /** Papasamyam (malefic equivalence) analysis for marriage compatibility. */
    suspend fun getPapasamyam(data: BirthData, language: String? = null): JsonObject =
        postJsonBody(
            "papasamyam_details",
            mapOf(
                "full_name" to data.requireName("papasamyam_details"),
                "lat" to data.lat,
                "year" to data.year,
                "month" to data.month,
                "min" to data.min,
                "hour" to data.hour,
                "ayanamsha" to DEFAULT_AYANAMSHA,
                "date" to data.day,
                "lon" to data.lon,
                "day" to data.day,
                "tzone" to data.tzone,
            ),
            language,
        )

    // ── Varshaphal (Solar Return) ──────────────────────────────────────────────

    /** Varshaphal chart overview. Pass yearCount to query a specific year. */
    suspend fun getVarshaphalDetails(data: BirthData, yearCount: Int? = null, language: String? = null): JsonObject =
        post("varshaphal_details", data.varshaphalBody(yearCount), language)

    /** Annual (Varshaphal) chart — whole-year divisional chart. */
    suspend fun getVarshaphalYearChart(data: BirthData, yearCount: Int? = null, language: String? = null): JsonObject =
        post("varshaphal_year_chart", data.varshaphalBody(yearCount), language)

    /** Monthly (Maasa) chart within the Varshaphal year. */
    suspend fun getVarshaphalMonthChart(data: BirthData, yearCount: Int? = null, language: String? = null): JsonElement =
        postElement("varshaphal_month_chart", data.varshaphalBody(yearCount), language)

    /** Planetary positions in the Varshaphal chart. */
    suspend fun getVarshaphalPlanets(data: BirthData, yearCount: Int? = null, language: String? = null): JsonElement =
        postElement("varshaphal_planets", data.varshaphalBody(yearCount), language)

    /** Muntha (annual ascendant) position and significance. */
    suspend fun getVarshaphalMuntha(data: BirthData, yearCount: Int? = null, language: String? = null): JsonElement =
        postElement("varshaphal_muntha", data.varshaphalBody(yearCount), language)

    /** Mudda Dasha (annual dasha) running during the Varshaphal year. */
    suspend fun getVarshaphalMuddaDasha(data: BirthData, yearCount: Int? = null, language: String? = null): JsonElement =
        postElement("varshaphal_mudda_dasha", data.varshaphalBody(yearCount), language)

    /** Panchavargeeya Bala (five-fold strength) in the Varshaphal. */
    suspend fun getVarshaphalPanchavargeeya(data: BirthData, yearCount: Int? = null, language: String? = null): JsonObject =
        post("varshaphal_panchavargeeya_bala", data.varshaphalBody(yearCount), language)

    /** Harsha Bala (strength indicator) in the Varshaphal. */
    suspend fun getVarshaphalHarshaBala(data: BirthData, yearCount: Int? = null, language: String? = null): JsonObject =
        post("varshaphal_harsha_bala", data.varshaphalBody(yearCount), language)

    /** Yoga formations in the Varshaphal chart. */
    suspend fun getVarshaphalYoga(data: BirthData, yearCount: Int? = null, language: String? = null): JsonElement =
        postElement("varshaphal_yoga", data.varshaphalBody(yearCount), language)

    /** Saham (Arabic Parts equivalent) positions in the Varshaphal. */
    suspend fun getVarshaphalSahamPoints(data: BirthData, yearCount: Int? = null, language: String? = null): JsonElement =
        postElement("varshaphal_saham_points", data.varshaphalBody(yearCount), language)

    // ── Reports ────────────────────────────────────────────────────────────────

    /** Interpretive report for the birth ascendant (Lagna). */
    suspend fun getAscendantReport(data: BirthData, language: String? = null): JsonObject =
        post("general_ascendant_report", data.ayanamshaBody(), language)

    /** Nakshatra (lunar mansion) analysis and characteristics. */
    suspend fun getNakshatraReport(data: BirthData, language: String? = null): JsonObject =
        post("general_nakshatra_report", data.nakshatraBody(), language)

    /** Interpretive report for a planet's house placement. */
    suspend fun getHouseReport(planet: PlanetName, data: BirthData, language: String? = null): JsonObject =
        post("general_house_report/$planet", data.ayanamshaBody(), language)

    /** Interpretive report for a planet's sign placement. */
    suspend fun getRashiReport(planet: PlanetName, data: BirthData, language: String? = null): JsonObject =
        post("general_rashi_report/$planet", data.ayanamshaBody(), language)

    // ── Biorhythm ──────────────────────────────────────────────────────────────

    /** Physical, emotional, and intellectual biorhythm cycles. */
    suspend fun getBiorhythm(data: BirthData, language: String? = null): JsonObject =
        post("biorhythm", data.ayanamshaBody(), language)

    /** Lunar biorhythm cycle based on moon phase at birth. */
    suspend fun getMoonBiorhythm(data: BirthData, language: String? = null): JsonObject =
        post("moon_biorhythm", data.ayanamshaBody(), language)
}
