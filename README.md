# AstrologyAPI Kotlin SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.astrologyapi/astrologyapi-kotlin.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.astrologyapi/astrologyapi-kotlin)
[![CI](https://github.com/astrologyapi/astrologyapi-kotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/astrologyapi/astrologyapi-kotlin/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Official Kotlin/Android SDK for [AstrologyAPI.com](https://astrologyapi.com) — 140+ Vedic and Western astrology endpoints, organised into 11 intuitive namespaces with full coroutine support.

Works on:
- Android (API 21+)
- Server-side JVM (Java 8+)

---

## Get API Access

1. Go to [astrologyapi.com](https://astrologyapi.com) and create a free account.
2. Navigate to your [Dashboard](https://astrologyapi.com/dashboard).
3. Copy your **API Key**.
4. Use this credential when constructing the SDK client (see Quick Start below).

---

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.astrologyapi:astrologyapi-kotlin:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.astrologyapi:astrologyapi-kotlin:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.astrologyapi</groupId>
    <artifactId>astrologyapi-kotlin</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Quick Start

```kotlin
import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.ChartId
import com.astrologyapi.sdk.models.ZodiacSign

suspend fun main() {
    val client = AstrologyAPI(
        AstrologyAPIConfig(
            apiKey = "ak-your_token_here",
        )
    )

    val birthData = BirthData(
        day = 10, month = 5, year = 1990,
        hour = 19, min = 55,
        lat = 19.20, lon = 72.83,
        tzone = 5.5,
    )

    // Vedic natal chart
    val lagnaChart = client.vedic.getChart(ChartId.D1, birthData)
    println(lagnaChart)

    // Daily horoscope for Aries
    val daily = client.horoscopes.getDaily(ZodiacSign.ARIES)
    println(daily)
}
```

---

## Authentication

The SDK uses token-based authentication with an API Key (usually starts with `ak-`).

### Direct Token

```kotlin
val client = AstrologyAPI(
    AstrologyAPIConfig(
        apiKey = "ak-your_token_here",
    )
)
```

### Environment Variables

Alternatively, set your API key in an environment variable and the SDK will pick it up automatically:

```bash
export ASTROLOGYAPI_API_KEY="ak-your_token_here"
```

Then construct the client:

```kotlin
val client = AstrologyAPI() // Reads from ASTROLOGYAPI_API_KEY
```

---

## Namespaces

### `vedic` — Vedic Astrology

Parashari and Jaimini systems — birth charts, divisional charts, all dasha systems, panchang, muhurta, matchmaking, varshaphal, doshas, remedies, and reports.

```kotlin
val birthData = BirthData(day=10, month=5, year=1990, hour=19, min=55, lat=19.20, lon=72.83, tzone=5.5)

// Birth chart
val birthDetails = client.vedic.getBirthDetails(birthData)

// Planetary positions
val planets = client.vedic.getPlanets(birthData)

// Divisional chart (D9 = Navamsa)
val navamsa = client.vedic.getChart(ChartId.D9, birthData)

// Current Vimshottari Dasha
val dasha = client.vedic.getCurrentDasha(birthData)

// Kalsarpa Dosha
val kalsarpa = client.vedic.getKalsarpaDosha(birthData)

// Basic Panchang
val panchang = client.vedic.getBasicPanchang(birthData)

// Gemstone recommendations
val gems = client.vedic.getGemSuggestion(birthData)

// Matchmaking
val matchData = MatchBirthData(male = birthData, female = femaleBirthData)
val compatibility = client.vedic.getMatchReport(matchData)

// Varshaphal (Solar Return)
val varshaphal = client.vedic.getVarshaphalDetails(birthData, yearCount = 34)
```

### `kp` — KP Astrology

Krishnamurti Paddhati — sub-lord theory, house cusps, significators.

```kotlin
val kpPlanets = client.kp.getPlanets(birthData)
val cusps = client.kp.getHouseCusps(birthData)
val horoscope = client.kp.getHoroscope(birthData)
val houseSignificators = client.kp.getHouseSignificator(birthData)
```

### `lalKitab` — Lal Kitab

Unconventional Vedic astrology with unique remedies.

```kotlin
val horoscope = client.lalKitab.getHoroscope(birthData)
val debts = client.lalKitab.getDebts(birthData)
val remedies = client.lalKitab.getRemedies(PlanetName.SATURN, birthData)
val planets = client.lalKitab.getPlanets(birthData)
```

### `horoscopes` — Sun-Sign Horoscopes

Daily, next-day, previous-day, consolidated, and monthly horoscopes.

```kotlin
val daily = client.horoscopes.getDaily(ZodiacSign.ARIES)
val tomorrow = client.horoscopes.getNext(ZodiacSign.LEO)
val monthly = client.horoscopes.getMonthly(ZodiacSign.CAPRICORN)
val nakshatra = client.horoscopes.getDailyNakshatra(birthData)
```

### `numerology` — Numerology

Both Vedic and Western numerological systems.

```kotlin
val numData = NumerologyData(day=10, month=5, year=1990, name="John Doe")

// Vedic
val table = client.numerology.getTable(numData)
val report = client.numerology.getReport(numData)

// Western
val lifePath = client.numerology.getLifepathNumber(numData)
val expression = client.numerology.getExpressionNumber(numData)
val soulUrge = client.numerology.getSoulUrgeNumber(numData)
val challenges = client.numerology.getChallengeNumbers(numData)
```

### `western` — Western Astrology

Tropical zodiac — natal charts, solar return, synastry, personality, moon phases.

```kotlin
// Natal chart
val planets = client.western.getPlanets(birthData)
val horoscope = client.western.getHoroscope(birthData)
val personality = client.western.getPersonality(birthData)

// Solar Return
val solarReturn = client.western.getSolarReturnDetails(birthData, year = 2025)

// Synastry
val couple = CoupleBirthData(person1 = birthData, person2 = partnerBirthData)
val synastry = client.western.getSynastry(couple)

// Compatibility
val compat = client.western.getZodiacCompatibility(ZodiacSign.ARIES, ZodiacSign.LEO)
```

### `westernTransit` — Western Transits

Tropical and natal transits at daily, weekly, and monthly granularity.

```kotlin
val daily = client.westernTransit.getDaily(birthData)
val weekly = client.westernTransit.getWeekly(birthData)
val natalDaily = client.westernTransit.getNatalDaily(birthData)
```

### `tarot` — Tarot

Card readings — general three-card spread or yes/no.

```kotlin
val reading = client.tarot.getPredictions()
val yesNo = client.tarot.getYesNo()
```

### `chinese` — Chinese Astrology

Chinese zodiac and annual forecasts.

```kotlin
val zodiac = client.chinese.getZodiac(birthData)
val forecast = client.chinese.getYearForecast(birthData)
```

### `pdf` — PDF Reports

Generate branded PDF reports as `ByteArray`. Save to disk or stream to clients.

```kotlin
// Vedic reports
val miniKundli: ByteArray = client.pdf.vedic.getMiniKundli(
    data = birthData,
    name = "Arjun Sharma",
    place = "Mumbai",
    branding = PDFBranding(
        companyName = "My Astro App",
        logoUrl = "https://example.com/logo.png",
        chartStyle = "north-indian",
    ),
)
File("kundli.pdf").writeBytes(miniKundli)

val matchReport: ByteArray = client.pdf.vedic.getMatchMaking(
    data = matchData,
    name = "Arjun",
    partnerName = "Priya",
)

// Western reports
val natalChart: ByteArray = client.pdf.western.getNatalChart(birthData, name = "John")
val synastryPdf: ByteArray = client.pdf.western.getSynastry(coupleBirthData)
```

### `location` — Location & Timezone

Geocoding and DST-aware timezone resolution.

```kotlin
// Search for a city
val geo = client.location.getGeoDetails("Mumbai")

// Get timezone offset (accounts for DST)
val tz = client.location.getTimezone(
    day = 10, month = 5, year = 1990,
    hour = 19, min = 55,
    lat = 19.20, lon = 72.83,
)
```

---

## Error Handling

All exceptions extend `AstrologyAPIException`. Catch the base class for general handling, or specific subclasses for fine-grained control:

```kotlin
import com.astrologyapi.sdk.errors.*

try {
    val result = client.vedic.getPlanets(birthData)
} catch (e: AuthenticationException) {
    // HTTP 401 — invalid apiKey
    println("Auth failed: ${e.message}")

} catch (e: ValidationException) {
    // HTTP 400/422 — bad request parameters
    println("Validation error on field '${e.field}': ${e.message}")

} catch (e: QuotaExceededException) {
    // HTTP 402 — monthly quota used up
    println("Quota exceeded. Upgrade at astrologyapi.com/pricing")

} catch (e: PlanRestrictedException) {
    // HTTP 403 — endpoint not in your current plan
    println("Endpoint not available on your plan: ${e.message}")

} catch (e: RateLimitException) {
    // HTTP 429 — too many requests
    val waitSeconds = e.retryAfter ?: 60
    println("Rate limited. Retry after $waitSeconds seconds.")

} catch (e: ServerException) {
    // HTTP 5xx — AstrologyAPI server error
    println("Server error ${e.status}: ${e.message}")

} catch (e: NetworkException) {
    // No internet, DNS failure, timeout
    println("Network error: ${e.message}")
    e.cause?.printStackTrace()

} catch (e: AstrologyAPIException) {
    // Any other SDK error
    println("SDK error (${e.status}): ${e.message}")
}
```

All exceptions expose:
- `message` — human-readable description (often the API's own message)
- `status` — HTTP status code (null for `NetworkException`)
- `body` — raw response body string for debugging

---

## PDF Reports

PDF endpoints return `ByteArray`. You can write to disk, serve over HTTP, or stream to a client:

```kotlin
// Write to disk
val bytes = client.pdf.vedic.getBasicHoroscope(birthData, name = "Arjun")
File("horoscope.pdf").writeBytes(bytes)

// Stream in a Ktor response
call.respondBytes(bytes, ContentType.Application.Pdf)

// Serve from a Spring Boot controller
ResponseEntity.ok()
    .contentType(MediaType.APPLICATION_PDF)
    .body(bytes)
```

---

## Language Support

Pass a BCP 47 language code to get localised responses where supported:

```kotlin
val hindiPlanets = client.vedic.getPlanets(birthData, language = "hi")
val marathiPanchang = client.vedic.getBasicPanchang(birthData, language = "mr")
val englishHoroscope = client.horoscopes.getDaily(ZodiacSign.ARIES, language = "en")
```

---

## Custom Requests

Access endpoints not yet covered by the SDK using `customRequest()`:

```kotlin
val result = client.customRequest(
    endpoint = "some_new_endpoint",
    body = mapOf("day" to 10, "month" to 5, "year" to 1990),
)
```

---

## Configuration

```kotlin
val client = AstrologyAPI(
    AstrologyAPIConfig(
        apiKey = "ak-your_token_here",
        version = "v1",                  // API version (default: "v1")
        connectTimeoutMs = 30_000L,      // Connection timeout in ms
        readTimeoutMs = 30_000L,         // Read timeout in ms
    )
)
```

---

## Contributing

1. Fork the repository on GitHub.
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes with tests.
4. Run tests: `./gradlew test`
5. Submit a pull request.

Please open an issue first for significant changes.

---

## Links

- [AstrologyAPI.com](https://astrologyapi.com) — Main website
- [Dashboard](https://astrologyapi.com/dashboard) — Get your API credentials
- [API Documentation](https://astrologyapi.com/docs) — Full endpoint reference
- [Pricing](https://astrologyapi.com/pricing) — Plans and quotas
- [Contact Support](mailto:support@astrologyapi.com) — support@astrologyapi.com

---

## License

MIT License — see [LICENSE](LICENSE) for details.
