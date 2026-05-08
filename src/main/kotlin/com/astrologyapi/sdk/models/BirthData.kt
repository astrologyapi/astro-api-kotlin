package com.astrologyapi.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Birth data required by most Vedic and Western astrology endpoints.
 */
data class BirthData(
    /** Day of birth (1–31) */
    val day: Int,
    /** Month of birth (1–12) */
    val month: Int,
    /** Year of birth (e.g. 1990) */
    val year: Int,
    /** Hour of birth in 24-hour format (0–23) */
    val hour: Int,
    /** Minute of birth (0–59) */
    val min: Int,
    /** Latitude of birth place (e.g. 19.20 for Mumbai) */
    val lat: Double,
    /** Longitude of birth place (e.g. 72.83 for Mumbai) */
    val lon: Double,
    /** UTC timezone offset in hours (e.g. 5.5 for IST, -5.0 for EST) */
    val tzone: Double,
    /** Optional profile name used by selected report/PDF endpoints. */
    val name: String? = null,
    /** Optional birthplace label used by selected report/PDF endpoints. */
    val place: String? = null,
    /** Optional gender used by selected report/PDF endpoints. */
    val gender: String? = null,
) {
    fun toMap(includeProfileFields: Boolean = false): Map<String, Any> = buildMap {
        put("day", day)
        put("month", month)
        put("year", year)
        put("hour", hour)
        put("min", min)
        put("lat", lat)
        put("lon", lon)
        put("tzone", tzone)

        if (includeProfileFields) {
            name?.let { put("name", it) }
            place?.let { put("place", it) }
            gender?.let { put("gender", it) }
        }
    }
}

/**
 * Two birth data objects for Vedic matchmaking / compatibility endpoints (male + female).
 */
data class MatchBirthData(
    val male: BirthData,
    val female: BirthData,
) {
    fun flatten(): Map<String, Any> {
        val m = male
        val f = female
        return mapOf(
            "m_day" to m.day, "m_month" to m.month, "m_year" to m.year,
            "m_hour" to m.hour, "m_min" to m.min,
            "m_lat" to m.lat, "m_lon" to m.lon, "m_tzone" to m.tzone,
            "f_day" to f.day, "f_month" to f.month, "f_year" to f.year,
            "f_hour" to f.hour, "f_min" to f.min,
            "f_lat" to f.lat, "f_lon" to f.lon, "f_tzone" to f.tzone,
        )
    }
}

/**
 * Two birth data objects for Western synastry / couple endpoints (person1 + person2).
 */
data class CoupleBirthData(
    val person1: BirthData,
    val person2: BirthData,
) {
    fun flatten(): Map<String, Any> {
        val p = person1
        val s = person2
        return mapOf(
            "p_day" to p.day, "p_month" to p.month, "p_year" to p.year,
            "p_hour" to p.hour, "p_min" to p.min,
            "p_lat" to p.lat, "p_lon" to p.lon, "p_tzone" to p.tzone,
            "s_day" to s.day, "s_month" to s.month, "s_year" to s.year,
            "s_hour" to s.hour, "s_min" to s.min,
            "s_lat" to s.lat, "s_lon" to s.lon, "s_tzone" to s.tzone,
        )
    }
}

/**
 * Input for numerology endpoints. Requires birth date and full name.
 */
data class NumerologyData(
    val day: Int,
    val month: Int,
    val year: Int,
    val name: String,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "day" to day, "month" to month, "year" to year, "name" to name,
    )
}

/**
 * White-label branding for PDF report endpoints. All fields are optional.
 */
data class PDFBranding(
    @SerializedName("logo_url")
    val logoUrl: String? = null,
    @SerializedName("company_name")
    val companyName: String? = null,
    @SerializedName("company_info")
    val companyInfo: String? = null,
    @SerializedName("domain_url")
    val domainUrl: String? = null,
    @SerializedName("company_email")
    val companyEmail: String? = null,
    @SerializedName("company_landline")
    val companyLandline: String? = null,
    @SerializedName("company_mobile")
    val companyMobile: String? = null,
    @SerializedName("footer_link")
    val footerLink: String? = null,
    /** "north-indian" | "south-indian" | "east-indian" */
    @SerializedName("chart_style")
    val chartStyle: String? = null,
) {
    fun toMap(): Map<String, Any> = buildMap {
        logoUrl?.let { put("logo_url", it) }
        companyName?.let { put("company_name", it) }
        companyInfo?.let { put("company_info", it) }
        domainUrl?.let { put("domain_url", it) }
        companyEmail?.let { put("company_email", it) }
        companyLandline?.let { put("company_landline", it) }
        companyMobile?.let { put("company_mobile", it) }
        footerLink?.let { put("footer_link", it) }
        chartStyle?.let { put("chart_style", it) }
    }
}
