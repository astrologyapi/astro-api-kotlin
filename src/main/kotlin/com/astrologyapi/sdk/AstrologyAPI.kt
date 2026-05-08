package com.astrologyapi.sdk

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.http.HttpExchangeObserver
import com.astrologyapi.sdk.models.ApiDomain
import com.astrologyapi.sdk.namespaces.*
import com.google.gson.JsonObject

/**
 * Official AstrologyAPI.com Kotlin SDK.
 *
 * Provides access to 140+ Vedic and Western astrology endpoints,
 * organised into 11 intuitive namespaces.
 *
 * Get your API credentials at [astrologyapi.com](https://astrologyapi.com/dashboard).
 *
 * ```kotlin
 * val client = AstrologyAPI(AstrologyAPIConfig(
 *     userId = "YOUR_USER_ID",
 *     apiKey = "YOUR_API_KEY",
 * ))
 *
 * val birthData = BirthData(day=10, month=5, year=1990, hour=19, min=55,
 *                           lat=19.20, lon=72.83, tzone=5.5)
 *
 * val chart = client.vedic.getChart(ChartId.D1, birthData)
 * val daily = client.horoscopes.getDaily(ZodiacSign.ARIES)
 * ```
 */
class AstrologyAPI private constructor(
    private val http: HttpClient,
) {

    constructor(config: AstrologyAPIConfig = AstrologyAPIConfig()) : this(HttpClient(config))

    internal constructor(
        config: AstrologyAPIConfig,
        observer: HttpExchangeObserver?,
    ) : this(HttpClient(config, observer))

    /** Vedic (Parashari & Jaimini) astrology — birth charts, dashas, doshas, panchang, matchmaking, varshaphal */
    val vedic = VedicNamespace(http)

    /** Krishnamurti Paddhati (KP) system — sub-lord theory, cusps, significators */
    val kp = KPNamespace(http)

    /** Lal Kitab system — horoscope, debts, remedies */
    val lalKitab = LalKitabNamespace(http)

    /** Daily, weekly, and monthly sun-sign & nakshatra predictions */
    val horoscopes = HoroscopesNamespace(http)

    /** Vedic and Western numerology — life path, expression, soul urge, and more */
    val numerology = NumerologyNamespace(http)

    /** Western (tropical) astrology — birth charts, solar return, synastry, personality, moon */
    val western = WesternNamespace(http)

    /** Western transit calculations — tropical and natal transits (daily, weekly, monthly) */
    val westernTransit = WesternTransitNamespace(http)

    /** Tarot card readings — general predictions and yes/no */
    val tarot = TarotNamespace(http)

    /** Chinese zodiac and annual forecast */
    val chinese = ChineseNamespace(http)

    /** PDF report generation — Vedic and Western branded reports */
    val pdf = PDFNamespace(http)

    /** Geographic lookup and timezone resolution */
    val location = LocationNamespace(http)

    /**
     * Make a custom request to any AstrologyAPI endpoint not yet covered by the SDK.
     *
     * @param endpoint Endpoint path (e.g. "some_new_endpoint")
     * @param body Request body fields
     * @param domain "JSON" for json.astrologyapi.com, "PDF" for pdf.astrologyapi.com
     * @param language Optional language code for localised responses
     */
    suspend fun customRequest(
        endpoint: String,
        body: Map<String, Any?> = emptyMap(),
        domain: ApiDomain = ApiDomain.JSON,
        language: String? = null,
    ): JsonObject = http.postJson(endpoint, body, language)
}
