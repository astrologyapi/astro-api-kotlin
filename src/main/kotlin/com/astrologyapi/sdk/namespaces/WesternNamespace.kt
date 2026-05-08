package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.CoupleBirthData
import com.astrologyapi.sdk.models.PlanetName
import com.astrologyapi.sdk.models.ZodiacSign
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** Western (tropical) astrology — birth charts, solar return, synastry, personality, moon. */
class WesternNamespace internal constructor(private val http: HttpClient) {

    private suspend fun post(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postForm(endpoint, body, language)

    private suspend fun postElement(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postElement(endpoint, body, language)

    // ── Core Chart ────────────────────────────────────────────────────────────

    /** Tropical (Western) planetary positions. */
    suspend fun getPlanets(data: BirthData, language: String? = null): JsonElement =
        postElement("planets/tropical", data.toMap(), language)

    /** Tropical house cusps (Placidus). */
    suspend fun getHouseCusps(data: BirthData, language: String? = null): JsonObject =
        post("house_cusps/tropical", data.toMap(), language)

    /** Complete Western horoscope — planets, houses, aspects. */
    suspend fun getHoroscope(data: BirthData, language: String? = null): JsonObject =
        post("western_horoscope", data.toMap(), language)

    /** SVG natal wheel chart. */
    suspend fun getNatalWheelChart(data: BirthData, language: String? = null): JsonObject =
        post("natal_wheel_chart", data.toMap(), language)

    /** Narrative interpretation of the natal chart. */
    suspend fun getNatalInterpretation(data: BirthData, language: String? = null): JsonObject =
        post("natal_chart_interpretation", data.toMap(), language)

    // ── House Reports ─────────────────────────────────────────────────────────

    /** Interpretive descriptions for all house cusps. */
    suspend fun getHouseCuspsReport(data: BirthData, language: String? = null): JsonElement =
        postElement("house_cusps_report/tropical", data.toMap(), language)

    /** Natal house cusp report with planetary influence. */
    suspend fun getNatalHouseCuspReport(data: BirthData, language: String? = null): JsonElement =
        postElement("natal_house_cusp_report", data.toMap(), language)

    /** Interpretive report for the Western ascendant sign. */
    suspend fun getAscendantReport(data: BirthData, language: String? = null): JsonObject =
        post("general_ascendant_report/tropical", data.toMap(), language)

    /** Report for a planet's sign placement in the tropical zodiac. */
    suspend fun getSignReport(planet: PlanetName, data: BirthData, language: String? = null): JsonObject =
        post("general_sign_report/tropical/$planet", data.toMap(), language)

    /** Report for a planet's house placement in the tropical chart. */
    suspend fun getHouseReport(planet: PlanetName, data: BirthData, language: String? = null): JsonObject =
        post("general_house_report/tropical/$planet", data.toMap(), language)

    // ── Moon ──────────────────────────────────────────────────────────────────

    /** Current moon phase and its astrological significance. */
    suspend fun getMoonPhase(data: BirthData, language: String? = null): JsonObject =
        post("moon_phase_report", data.toMap(), language)

    /** Lunar metrics — moon sign, nakshatra, tithi. */
    suspend fun getLunarMetrics(data: BirthData, language: String? = null): JsonObject =
        post("lunar_metrics", data.toMap(), language)

    // ── Solar Return ──────────────────────────────────────────────────────────

    /** Solar Return chart details. */
    suspend fun getSolarReturnDetails(data: BirthData, year: Int? = null, language: String? = null): JsonObject =
        post("solar_return_details", data.toMap() + mapOfNotNull("solar_year" to year), language)

    /** Planetary positions in the Solar Return chart. */
    suspend fun getSolarReturnPlanets(data: BirthData, year: Int? = null, language: String? = null): JsonElement =
        postElement("solar_return_planets", data.toMap() + mapOfNotNull("solar_year" to year), language)

    /** House cusps in the Solar Return chart. */
    suspend fun getSolarReturnHouseCusps(data: BirthData, year: Int? = null, language: String? = null): JsonObject =
        post("solar_return_house_cusps", data.toMap() + mapOfNotNull("solar_year" to year), language)

    /** Interpretive report for each planet in the Solar Return chart. */
    suspend fun getSolarReturnPlanetReport(data: BirthData, year: Int? = null, language: String? = null): JsonElement =
        postElement("solar_return_planet_report", data.toMap() + mapOfNotNull("solar_year" to year), language)

    /** Aspects between Solar Return planets and natal planets. */
    suspend fun getSolarReturnPlanetAspects(data: BirthData, year: Int? = null, language: String? = null): JsonElement =
        postElement("solar_return_planet_aspects", data.toMap() + mapOfNotNull("solar_year" to year), language)

    /** Comprehensive aspects report for the Solar Return chart. */
    suspend fun getSolarReturnAspectsReport(data: BirthData, year: Int? = null, language: String? = null): JsonElement =
        postElement("solar_return_aspects_report", data.toMap() + mapOfNotNull("solar_year" to year), language)

    // ── Personality & Life Reports ────────────────────────────────────────────

    /** Personality analysis based on the tropical natal chart. */
    suspend fun getPersonality(data: BirthData, language: String? = null): JsonObject =
        post("personality_report/tropical", data.toMap(), language)

    /** Romantic personality — how the person behaves in love. */
    suspend fun getRomanticPersonality(data: BirthData, language: String? = null): JsonObject =
        post("romantic_personality_report/tropical", data.toMap(), language)

    /** Friendship profile — social style and compatible friend types. */
    suspend fun getFriendship(data: CoupleBirthData, language: String? = null): JsonObject =
        post("friendship_report/tropical", data.flatten(), language)

    /** Romantic forecast for the current period. */
    suspend fun getRomanticForecast(data: BirthData, language: String? = null): JsonObject =
        post("romantic_forecast_report/tropical", data.toMap(), language)

    /** Karma and destiny report. */
    suspend fun getKarmaDestiny(data: CoupleBirthData, language: String? = null): JsonObject =
        post("karma_destiny_report/tropical", data.flatten(), language)
    // ── Compatibility ─────────────────────────────────────────────────────────

    /** Sun-sign compatibility score between two zodiac signs. */
    suspend fun getZodiacCompatibility(zodiac: ZodiacSign, partnerZodiac: ZodiacSign, language: String? = null): JsonObject =
        post("zodiac_compatibility/$zodiac/$partnerZodiac", emptyMap(), language)

    /** Synastry horoscope — aspect analysis between two natal charts. */
    suspend fun getSynastry(data: CoupleBirthData, language: String? = null): JsonObject =
        post("synastry_horoscope", data.flatten(), language)

    /** Composite chart — the midpoint chart representing the relationship. */
    suspend fun getComposite(data: CoupleBirthData, language: String? = null): JsonObject =
        post("composite_horoscope", data.flatten(), language)
}
