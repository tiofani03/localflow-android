package com.localflow.sdk.data.network

import com.localflow.sdk.LocalflowConfig
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalflowApiImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: LocalflowApiImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()
        val config = LocalflowConfig.Builder(
            apiKey = "loc_live_test1234567890123456789012345678901234567890",
            baseUrl = baseUrl
        ).build()

        api = LocalflowApiImpl(config)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testBootstrapSuccess() = runBlocking {
        val jsonResponse = """
            {
              "project": {
                "name": "Locaflow Demo",
                "slug": "locaflow-demo",
                "description": "Localization delivery"
              },
              "defaultLanguage": {
                "code": "en",
                "name": "English",
                "isDefault": true
              },
              "availableLanguages": [
                { "code": "en", "name": "English", "isDefault": true },
                { "code": "id", "name": "Indonesian", "isDefault": false }
              ],
              "latestVersion": 3,
              "publishedAt": "2026-05-22T13:00:00.000Z",
              "cacheTtlSeconds": 300,
              "etag": "\"abc123\"",
              "checksum": "sha256-value"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("ETag", "\"abc123\"")
                .addHeader("X-Localization-Version", "3")
        )

        val result = api.bootstrap()

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("Locaflow Demo", data.project.name)
        assertEquals(3, data.latestVersion)
        assertEquals("\"abc123\"", data.etag)
        assertEquals("en", data.defaultLanguage?.code)
        assertEquals(2, data.availableLanguages.size)
    }

    @Test
    fun testGetLatestVersionHasUpdate() = runBlocking {
        val jsonResponse = """
            {
              "projectSlug": "locaflow-demo",
              "version": 3,
              "publishedAt": "2026-05-22T13:00:00.000Z",
              "etag": "\"abc123\"",
              "checksum": "sha256-value",
              "requestedVersion": 2,
              "hasUpdate": true
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
        )

        val result = api.getLatestVersion(currentVersion = 2)

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertTrue(data.hasUpdate)
        assertEquals(3, data.version)
        assertEquals(2, data.requestedVersion)
    }

    @Test
    fun testGetLocalizationsNotModified() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(304)
        )

        val result = api.getLocalizations(etag = "\"all-etag\"")

        assertTrue(result is ApiResult.NotModified)
    }

    @Test
    fun testGetLanguageBundleSuccess() = runBlocking {
        val jsonResponse = """
            {
              "project": {
                "slug": "locaflow-demo",
                "name": "Locaflow Demo"
              },
              "language": {
                "code": "en",
                "name": "English",
                "isDefault": true
              },
              "version": 3,
              "publishedAt": "2026-05-22T13:00:00.000Z",
              "etag": "\"bundle-en-etag\"",
              "checksum": "bundle-en-checksum",
              "localizations": {
                "home.title": "Home",
                "home.subtitle": "Welcome back"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("ETag", "\"bundle-en-etag\"")
                .addHeader("X-Localization-Version", "3")
        )

        val result = api.getLanguageBundle("en", etag = null)

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("Home", data.localizations["home.title"])
        assertEquals("Welcome back", data.localizations["home.subtitle"])
        assertEquals("en", data.language.code)
    }

    @Test
    fun testInvalidApiKeyError() = runBlocking {
        val errorResponse = """
            {
              "error": {
                "code": "INVALID_API_KEY",
                "message": "The provided mobile delivery key is invalid."
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
        )

        val result = api.bootstrap()

        assertTrue(result is ApiResult.Error)
        val err = result as ApiResult.Error
        assertEquals("INVALID_API_KEY", err.code)
        assertEquals("The provided mobile delivery key is invalid.", err.message)
        assertEquals(401, err.httpStatus)
    }
}
