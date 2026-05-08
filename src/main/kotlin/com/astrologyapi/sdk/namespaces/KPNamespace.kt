package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** KP (Krishnamurti Paddhati) system — sub-lord theory, cusps, significators. */
class KPNamespace internal constructor(private val http: HttpClient) {

    private suspend fun post(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postForm(endpoint, body, language)

    private suspend fun postElement(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postElement(endpoint, body, language)

    /** KP planetary positions with sub-lord, star-lord, and sign data. */
    suspend fun getPlanets(data: BirthData, language: String? = null): JsonElement =
        postElement("kp_planets", data.ayanamshaBody(), language)

    /** KP house cusps with sub-lord and star-lord for each cusp. */
    suspend fun getHouseCusps(data: BirthData, language: String? = null): JsonElement =
        postElement("kp_house_cusps", data.ayanamshaBody(), language)

    /** Full KP birth chart including planets and house cusps. */
    suspend fun getBirthChart(data: BirthData, language: String? = null): JsonElement =
        postElement("kp_birth_chart", data.ayanamshaBody(), language)

    /** KP significators for each house (1–12). */
    suspend fun getHouseSignificator(data: BirthData, language: String? = null): JsonElement =
        postElement("kp_house_significator", data.ayanamshaBody(), language)

    /** KP significators for each planet. */
    suspend fun getPlanetSignificator(data: BirthData, language: String? = null): JsonElement =
        postElement("kp_planet_significator", data.ayanamshaBody(), language)

    /** Complete KP horoscope combining all KP chart data. */
    suspend fun getHoroscope(data: BirthData, language: String? = null): JsonObject =
        post("kp_horoscope", data.ayanamshaBody() + mapOf("aspects" to true), language)
}
