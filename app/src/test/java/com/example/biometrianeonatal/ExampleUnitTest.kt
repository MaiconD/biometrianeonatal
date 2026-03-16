package com.example.biometrianeonatal

import com.example.biometrianeonatal.core.sensors.FakeSensorAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class ExampleUnitTest {

    private val sensorAdapter = FakeSensorAdapter()

    @Test
    fun capture_generates_preview_with_expected_metadata() = runBlocking {
        val capture = sensorAdapter.capture(
            sessionId = "session-01",
            fingerCode = "POLEGAR_DIREITO",
        )

        assertEquals("session-01", capture.sessionId)
        assertEquals("POLEGAR_DIREITO", capture.fingerCode)
        assertTrue(capture.qualityScore in 78..98)
        assertTrue(capture.fps in listOf(8, 10))
        assertTrue(capture.resolution in listOf("640x480", "1280x720"))
        assertTrue(String(capture.imageBytes).contains("session=session-01"))
        assertFalse(capture.templateBase64.isBlank())
    }

    @Test
    fun capture_creates_distinct_image_payloads_between_invocations() = runBlocking {
        val first = sensorAdapter.capture("session-01", "POLEGAR_DIREITO")
        val second = sensorAdapter.capture("session-01", "POLEGAR_DIREITO")

        assertTrue(first.imageBytes.isNotEmpty())
        assertTrue(second.imageBytes.isNotEmpty())
        assertFalse(first.imageBytes.contentEquals(second.imageBytes))
    }
}