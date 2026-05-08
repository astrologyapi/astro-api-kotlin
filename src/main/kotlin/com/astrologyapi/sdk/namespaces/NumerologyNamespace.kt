package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.NumerologyData
import com.google.gson.JsonObject

/** Vedic and Western numerological calculations. */
class NumerologyNamespace internal constructor(private val http: HttpClient) {

    private suspend fun post(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postForm(endpoint, body, language)

    private suspend fun postVedic(endpoint: String, data: NumerologyData, language: String?) =
        post(endpoint, data.toMap(), language)

    private suspend fun postWestern(endpoint: String, data: NumerologyData, language: String?) =
        post(endpoint, data.westernBody(), language)

    private suspend fun postWestern(endpoint: String, data: BirthData, language: String?) =
        post(endpoint, data.westernNumerologyBody(endpoint), language)

    // ── Vedic Numerology ───────────────────────────────────────────────────────

    /** Vedic numerology table — Psychic, Destiny, Name, and Kua numbers. */
    suspend fun getTable(data: NumerologyData, language: String? = null): JsonObject =
        postVedic("numero_table", data, language)

    /** Comprehensive Vedic numerology report. */
    suspend fun getReport(data: NumerologyData, language: String? = null): JsonObject =
        postVedic("numero_report", data, language)

    /** Favourable time periods based on numerological cycles. */
    suspend fun getFavTime(data: NumerologyData, language: String? = null): JsonObject =
        postVedic("numero_fav_time", data, language)

    /** Vastu and place suitability based on numerology. */
    suspend fun getPlaceVastu(data: NumerologyData, language: String? = null): JsonObject =
        postVedic("numero_place_vastu", data, language)

    /** Recommended fasts (vrats) based on numerological analysis. */
    suspend fun getFastsReport(data: NumerologyData, language: String? = null): JsonObject =
        postVedic("numero_fasts_report", data, language)

    /** Favourable deity / ruling lord based on numerology. */
    suspend fun getFavLord(data: NumerologyData, language: String? = null): JsonObject =
        postVedic("numero_fav_lord", data, language)

    /** Recommended mantras based on numerological profile. */
    suspend fun getFavMantra(data: NumerologyData, language: String? = null): JsonObject =
        postVedic("numero_fav_mantra", data, language)

    /** Daily numerology prediction. */
    suspend fun getDailyPrediction(data: NumerologyData, language: String? = null): JsonObject =
        postVedic("numero_prediction/daily", data, language)

    // ── Western Numerology ─────────────────────────────────────────────────────

    /** All core Western numerological numbers in one response. */
    suspend fun getNumerologicalNumbers(data: NumerologyData, language: String? = null): JsonObject =
        postWestern("numerological_numbers", data, language)

    /** Life Path Number — the most significant number in Western numerology. */
    suspend fun getLifepathNumber(data: NumerologyData, language: String? = null): JsonObject =
        postWestern("lifepath_number", data, language)

    /** Personality Number — derived from consonants in the name. */
    suspend fun getPersonalityNumber(data: NumerologyData, language: String? = null): JsonObject =
        postWestern("personality_number", data, language)

    /** Expression (Destiny) Number — derived from the full name. */
    suspend fun getExpressionNumber(data: NumerologyData, language: String? = null): JsonObject =
        postWestern("expression_number", data, language)

    /** Soul Urge (Heart's Desire) Number — derived from vowels in the name. */
    suspend fun getSoulUrgeNumber(data: NumerologyData, language: String? = null): JsonObject =
        postWestern("soul_urge_number", data, language)

    /** Challenge Numbers — obstacles to overcome in each life stage. */
    suspend fun getChallengeNumbers(data: NumerologyData, language: String? = null): JsonObject =
        postWestern("challenge_numbers", data, language)

    /** Personal Day Number — the numerological energy of a specific day. */
    suspend fun getPersonalDay(data: BirthData, language: String? = null): JsonObject =
        postWestern("personal_day_prediction", data, language)

    /** Personal Month Number. */
    suspend fun getPersonalMonth(data: BirthData, language: String? = null): JsonObject =
        postWestern("personal_month_prediction", data, language)

    /** Personal Year Number. */
    suspend fun getPersonalYear(data: BirthData, language: String? = null): JsonObject =
        postWestern("personal_year_prediction", data, language)
}
