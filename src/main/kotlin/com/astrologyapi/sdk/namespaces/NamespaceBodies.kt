package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.MatchBirthData
import com.astrologyapi.sdk.models.NumerologyData

internal const val DEFAULT_AYANAMSHA = "LAHIRI"

internal fun BirthData.ayanamshaBody(
    ayanamsha: String = DEFAULT_AYANAMSHA,
): Map<String, Any?> = toMap() + mapOf("ayanamsha" to ayanamsha)

internal fun BirthData.nakshatraBody(
    gender: String? = this.gender,
    ayanamsha: String = DEFAULT_AYANAMSHA,
): Map<String, Any?> = toMap() + mapOfNotNull(
    "gender" to gender,
    "ayanamsha" to ayanamsha,
)

internal fun MatchBirthData.ayanamshaBody(
    ayanamsha: String = DEFAULT_AYANAMSHA,
): Map<String, Any?> = flatten() + mapOf("ayanamsha" to ayanamsha)

internal fun BirthData.varshaphalBody(
    yearCount: Int? = null,
    ayanamsha: String = DEFAULT_AYANAMSHA,
): Map<String, Any?> = toMap() + mapOfNotNull(
    "ayanamsha" to ayanamsha,
    "varshaphal_year" to yearCount?.let { year + it },
)

internal fun BirthData.requireName(endpoint: String): String =
    name?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("$endpoint requires BirthData.name.")

internal fun NumerologyData.westernBody(): Map<String, Any> = mapOf(
    "day" to day,
    "month" to month,
    "year" to year,
    "date" to day,
    "full_name" to name,
)

internal fun BirthData.westernNumerologyBody(endpoint: String): Map<String, Any> = mapOf(
    "day" to day,
    "month" to month,
    "year" to year,
    "date" to day,
    "full_name" to requireName(endpoint),
)

internal fun <K, V : Any> mapOfNotNull(vararg pairs: Pair<K, V?>): Map<K, V> =
    pairs.filter { it.second != null }.associate { it.first to it.second!! }
