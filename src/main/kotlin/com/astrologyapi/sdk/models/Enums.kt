package com.astrologyapi.sdk.models

/** Divisional (Varga) chart identifiers for Vedic astrology. */
enum class ChartId(val id: String) {
    D1("D1"), D2("D2"), D3("D3"), D4("D4"), D5("D5"),
    D7("D7"), D9("D9"), D10("D10"), D12("D12"), D16("D16"),
    D20("D20"), D24("D24"), D27("D27"), D30("D30"),
    D40("D40"), D45("D45"), D60("D60");

    override fun toString(): String = id
}

/** Western / tropical zodiac sign names. */
enum class ZodiacSign(val sign: String) {
    ARIES("aries"), TAURUS("taurus"), GEMINI("gemini"),
    CANCER("cancer"), LEO("leo"), VIRGO("virgo"),
    LIBRA("libra"), SCORPIO("scorpio"), SAGITTARIUS("sagittarius"),
    CAPRICORN("capricorn"), AQUARIUS("aquarius"), PISCES("pisces");

    override fun toString(): String = sign
}

/** Planet names used in parameterised endpoints. */
enum class PlanetName(val planet: String) {
    SUN("sun"), MOON("moon"), MARS("mars"), MERCURY("mercury"),
    JUPITER("jupiter"), VENUS("venus"), SATURN("saturn"),
    RAHU("rahu"), KETU("ketu"), ASCENDANT("ascendant");

    override fun toString(): String = planet
}

/** API domain — selects the base URL. */
enum class ApiDomain { JSON, PDF }
