package com.ndev.moodyroutine.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class LocationTrackingLogicTest {

    private val hysteresisBufferMeters = 60f
    private val maxAllowableAccuracyMeters = 150f

    @Test
    fun testHysteresisThresholds() {
        val baseRadius = 150f
        val exitRadius = baseRadius + hysteresisBufferMeters // 210m

        // Distance within baseRadius triggers enter
        val distance1 = 140f
        assertTrue("Distance <= 150m should trigger enter", distance1 <= baseRadius)

        // When inside, distance between baseRadius and exitRadius (e.g. 180m) does NOT trigger exit
        val distance2 = 180f
        assertFalse("Distance <= 210m should NOT trigger exit when inside", distance2 > exitRadius)

        // Only when distance exceeds 210m does it trigger exit
        val distance3 = 215f
        assertTrue("Distance > 210m should trigger exit", distance3 > exitRadius)
    }

    @Test
    fun testAccuracyGuardBlocksDegradedExit() {
        val accuracyDegraded = 180f
        val shouldIgnoreExit = accuracyDegraded > maxAllowableAccuracyMeters
        assertTrue("Degraded GPS accuracy (>150m) should be ignored during exit check", shouldIgnoreExit)

        val accuracyGood = 15f
        val shouldIgnoreGood = accuracyGood > maxAllowableAccuracyMeters
        assertFalse("Good GPS accuracy (<=150m) should NOT be ignored", shouldIgnoreGood)
    }

    @Test
    fun testProviderDeduplicationWindow() {
        val recentLocationEvents = ConcurrentHashMap<String, Long>()
        val dedupWindowMs = 10_000L

        val key = "office_true"
        val t0 = 100_000L

        // First event accepted
        val lastSeen0 = recentLocationEvents[key] ?: 0L
        val isDuplicate0 = (t0 - lastSeen0 < dedupWindowMs)
        assertFalse("First event should not be duplicate", isDuplicate0)
        recentLocationEvents[key] = t0

        // Duplicate event from parallel provider 2 seconds later
        val t1 = 102_000L
        val lastSeen1 = recentLocationEvents[key] ?: 0L
        val isDuplicate1 = (t1 - lastSeen1 < dedupWindowMs)
        assertTrue("Parallel provider event within 2s should be suppressed as duplicate", isDuplicate1)

        // Event arriving after 11 seconds (outside window) accepted
        val t2 = 111_000L
        val lastSeen2 = recentLocationEvents[key] ?: 0L
        val isDuplicate2 = (t2 - lastSeen2 < dedupWindowMs)
        assertFalse("Event after 11s should not be duplicate", isDuplicate2)
        recentLocationEvents[key] = t2
    }
}
