package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.CoupleBirthData
import com.astrologyapi.sdk.models.MatchBirthData
import com.astrologyapi.sdk.models.PDFBranding

/**
 * PDF namespace — generate branded PDF astrology reports.
 * All methods return a [ByteArray] containing the PDF binary.
 */
class PDFNamespace internal constructor(http: HttpClient) {

    /** Vedic PDF reports */
    val vedic: VedicPDF = VedicPDF(http)

    /** Western PDF reports */
    val western: WesternPDF = WesternPDF(http)
}

class VedicPDF internal constructor(private val http: HttpClient) {

    /** Mini Kundli PDF — compact birth chart summary. */
    suspend fun getMiniKundli(
        data: BirthData,
        name: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        language: String? = null,
    ): ByteArray = http.postPdf(
        "mini_horoscope_pdf",
        buildVedicBody(data, place, branding, language),
        language,
    )

    /** Basic horoscope PDF — standard birth chart with planetary positions and dasha table. */
    suspend fun getBasicHoroscope(
        data: BirthData,
        name: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        language: String? = null,
    ): ByteArray = http.postPdf(
        "basic_horoscope_pdf",
        buildVedicBody(data, place, branding, language),
        language,
    )

    /** Professional horoscope PDF — comprehensive report. */
    suspend fun getProfessionalHoroscope(
        data: BirthData,
        name: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        language: String? = null,
    ): ByteArray = http.postPdf(
        "pro_horoscope_pdf",
        buildVedicBody(data, place, branding, language),
        language,
    )

    /** Match Making PDF — compatibility report for two individuals. */
    suspend fun getMatchMaking(
        data: MatchBirthData,
        name: String? = null,
        partnerName: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        language: String? = null,
    ): ByteArray {
        val (maleFirstName, maleLastName) = splitName(name ?: data.male.name)
        val (femaleFirstName, femaleLastName) = splitName(partnerName ?: data.female.name)
        val body = mutableMapOf<String, Any?>(
            "m_day" to data.male.day,
            "m_month" to data.male.month,
            "m_year" to data.male.year,
            "m_hour" to data.male.hour,
            "m_minute" to data.male.min,
            "m_latitude" to data.male.lat,
            "m_longitude" to data.male.lon,
            "m_timezone" to data.male.tzone,
            "f_day" to data.female.day,
            "f_month" to data.female.month,
            "f_year" to data.female.year,
            "f_hour" to data.female.hour,
            "f_minute" to data.female.min,
            "f_latitude" to data.female.lat,
            "f_longitude" to data.female.lon,
            "f_timezone" to data.female.tzone,
            "language" to language,
            "ashtakoot" to true,
            "papasyam" to true,
            "dashakoot" to true,
        )
        maleFirstName?.let { body["m_first_name"] = it }
        maleLastName?.let { body["m_last_name"] = it }
        femaleFirstName?.let { body["f_first_name"] = it }
        femaleLastName?.let { body["f_last_name"] = it }
        (data.male.place ?: place)?.let { body["m_place"] = it }
        (data.female.place ?: place)?.let { body["f_place"] = it }
        brandingBody(
            branding = branding,
            uppercaseChartStyle = true,
            includeCompanyFields = false,
            includeChartStyle = true,
        ).let { body.putAll(it) }
        return http.postPdf("match_making_pdf", body, language)
    }

    private fun buildVedicBody(
        data: BirthData,
        place: String?,
        branding: PDFBranding?,
        language: String?,
    ): Map<String, Any?> {
        val body = mutableMapOf<String, Any?>(
            "day" to data.day,
            "month" to data.month,
            "year" to data.year,
            "hour" to data.hour,
            "min" to data.min,
            "lat" to data.lat,
            "lon" to data.lon,
            "tzone" to data.tzone,
            "language" to language,
        )
        data.gender?.let { body["gender"] = it }
        (place ?: data.place)?.let { body["place"] = it }
        body.putAll(
            brandingBody(
                branding = branding,
                uppercaseChartStyle = true,
                includeCompanyFields = true,
                includeChartStyle = true,
            )
        )
        return body
    }
}

class WesternPDF internal constructor(private val http: HttpClient) {

    /** Western natal chart PDF. */
    suspend fun getNatalChart(
        data: BirthData,
        name: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        language: String? = null,
    ): ByteArray = http.postPdf(
        "natal_horoscope_report/tropical",
        buildWesternBody(data, name, place, branding, language),
        language,
    )

    /** Life forecast PDF — transit-based annual forecast. */
    suspend fun getLifeForecast(
        data: BirthData,
        name: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        language: String? = null,
    ): ByteArray = http.postPdf(
        "life_forecast_report/tropical",
        buildWesternBody(data, name, place, branding, language),
        language,
    )

    /** Solar Return PDF — annual solar return chart and interpretation. */
    suspend fun getSolarReturn(
        data: BirthData,
        name: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        language: String? = null,
    ): ByteArray = getSolarReturn(
        data = data,
        name = name,
        place = place,
        branding = branding,
        solarYear = null,
        language = language,
    )

    /** Solar Return PDF — annual solar return chart and interpretation. */
    suspend fun getSolarReturn(
        data: BirthData,
        name: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        solarYear: Int? = null,
        language: String? = null,
    ): ByteArray = http.postPdf(
        "solar_return_report/tropical",
        buildWesternBody(data, name, place, branding, language, solarYear),
        language,
    )

    /** Synastry couple PDF — relationship compatibility report. */
    suspend fun getSynastry(
        data: CoupleBirthData,
        name: String? = null,
        partnerName: String? = null,
        place: String? = null,
        branding: PDFBranding? = null,
        language: String? = null,
    ): ByteArray {
        val (primaryFirstName, primaryLastName) = splitName(name ?: data.person1.name)
        val (secondaryFirstName, secondaryLastName) = splitName(partnerName ?: data.person2.name)
        val body = mutableMapOf<String, Any?>(
            "p_day" to data.person1.day,
            "p_month" to data.person1.month,
            "p_year" to data.person1.year,
            "p_hour" to data.person1.hour,
            "p_minute" to data.person1.min,
            "p_latitude" to data.person1.lat,
            "p_longitude" to data.person1.lon,
            "p_timezone" to data.person1.tzone,
            "s_day" to data.person2.day,
            "s_month" to data.person2.month,
            "s_year" to data.person2.year,
            "s_hour" to data.person2.hour,
            "s_minute" to data.person2.min,
            "s_latitude" to data.person2.lat,
            "s_longitude" to data.person2.lon,
            "s_timezone" to data.person2.tzone,
            "language" to language,
        )
        primaryFirstName?.let { body["p_first_name"] = it }
        primaryLastName?.let { body["p_last_name"] = it }
        secondaryFirstName?.let { body["s_first_name"] = it }
        secondaryLastName?.let { body["s_last_name"] = it }
        (data.person1.place ?: place)?.let { body["p_place"] = it }
        (data.person2.place ?: place)?.let { body["s_place"] = it }
        body.putAll(
            brandingBody(
                branding = branding,
                includeCompanyFields = true,
                includeChartStyle = false,
            )
        )
        return http.postPdf("synastry_couple_report/tropical", body, language)
    }

    private fun buildWesternBody(
        data: BirthData,
        name: String?,
        place: String?,
        branding: PDFBranding?,
        language: String?,
        solarYear: Int? = null,
    ): Map<String, Any?> {
        val body = mutableMapOf<String, Any?>(
            "day" to data.day,
            "month" to data.month,
            "year" to data.year,
            "hour" to data.hour,
            "minute" to data.min,
            "latitude" to data.lat,
            "longitude" to data.lon,
            "timezone" to data.tzone,
            "language" to language,
        )
        (name ?: data.name)?.let { body["name"] = it }
        (place ?: data.place)?.let { body["place"] = it }
        solarYear?.let { body["solar_year"] = it }
        body.putAll(
            brandingBody(
                branding = branding,
                includeCompanyFields = true,
                includeChartStyle = false,
            )
        )
        return body
    }
}

private fun brandingBody(
    branding: PDFBranding?,
    uppercaseChartStyle: Boolean = false,
    includeCompanyFields: Boolean = true,
    includeChartStyle: Boolean = true,
): Map<String, Any?> {
    if (branding == null) return emptyMap()
    val body = branding.toMap().toMutableMap()
    if (!includeCompanyFields) {
        body.remove("company_name")
        body.remove("company_info")
        body.remove("company_email")
        body.remove("company_landline")
        body.remove("company_mobile")
    }
    if (!includeChartStyle) {
        body.remove("chart_style")
    }
    if (uppercaseChartStyle) {
        val chartStyle = body["chart_style"] as? String
        if (chartStyle != null) {
            body["chart_style"] = chartStyle.uppercase()
        }
    }
    return body
}

private fun splitName(name: String?): Pair<String?, String?> {
    val value = name?.trim().orEmpty()
    if (value.isEmpty()) return null to null
    val parts = value.split(Regex("\\s+"), limit = 2)
    val first = parts[0].takeIf { it.isNotBlank() }
    val last = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
    return first to last
}
