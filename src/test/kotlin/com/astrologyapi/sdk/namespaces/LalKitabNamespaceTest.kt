package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.PlanetName
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class LalKitabNamespaceTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AstrologyAPI

    private val birth = BirthData(
        day = 10, month = 5, year = 1990,
        hour = 19, min = 55, lat = 19.20, lon = 72.83, tzone = 5.5,
    )

    @BeforeEach fun setUp() {
        server = MockWebServer()
        server.start()
        client = AstrologyAPI(AstrologyAPIConfig(
            userId = "u", apiKey = "k",
            baseJsonUrl = server.url("/").toString(),
        ))
    }

    @AfterEach fun tearDown() { server.shutdown() }

    private fun ok() = server.enqueue(
        MockResponse().setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("""{"ok":true}""")
    )

    @Test fun `getHoroscope posts to lalkitab_horoscope`() = runTest {
        ok()
        client.lalKitab.getHoroscope(birth)
        assertTrue(server.takeRequest().path!!.contains("lalkitab_horoscope"))
    }

    @Test fun `getDebts posts to lalkitab_debts`() = runTest {
        ok()
        client.lalKitab.getDebts(birth)
        assertTrue(server.takeRequest().path!!.contains("lalkitab_debts"))
    }

    @Test fun `getRemedies includes planet in path`() = runTest {
        ok()
        client.lalKitab.getRemedies(PlanetName.SATURN, birth)
        assertTrue(server.takeRequest().path!!.contains("lalkitab_remedies/saturn"))
    }

    @Test fun `getRemedies uses correct planet name`() = runTest {
        ok()
        client.lalKitab.getRemedies(PlanetName.MARS, birth)
        assertTrue(server.takeRequest().path!!.contains("lalkitab_remedies/mars"))
    }

    @Test fun `getHouses posts to lalkitab_houses`() = runTest {
        ok()
        client.lalKitab.getHouses(birth)
        assertTrue(server.takeRequest().path!!.contains("lalkitab_houses"))
    }

    @Test fun `getPlanets posts to lalkitab_planets`() = runTest {
        ok()
        client.lalKitab.getPlanets(birth)
        assertTrue(server.takeRequest().path!!.contains("lalkitab_planets"))
    }

    @Test fun `BirthData fields appear in request body`() = runTest {
        ok()
        client.lalKitab.getHoroscope(birth)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("day=10"))
        assertTrue(body.contains("lat=19.2"))
        assertTrue(body.contains("ayanamsha=LAHIRI"))
    }

    @Test fun `getHoroscope preserves array responses`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""[{"sign":1}]""")
        )
        val response = client.lalKitab.getHoroscope(birth)
        assertTrue(response.isJsonArray)
    }
}
