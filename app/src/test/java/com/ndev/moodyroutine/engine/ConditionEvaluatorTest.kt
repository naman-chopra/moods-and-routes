package com.ndev.moodyroutine.engine

import com.ndev.moodyroutine.data.model.TriggerConfig
import com.ndev.moodyroutine.data.model.TriggerType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionEvaluatorTest {

    private val evaluator = ConditionEvaluator()

    @Test
    fun testBatteryLevelBelow() {
        val trigger = TriggerConfig(
            type = TriggerType.BATTERY_LEVEL,
            params = mapOf("level" to "30", "comparison" to "below")
        )

        assertTrue(evaluator.evaluate(AutomationEvent.BatteryEvent(30, false), trigger))
        assertTrue(evaluator.evaluate(AutomationEvent.BatteryEvent(15, false), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.BatteryEvent(31, false), trigger))
    }

    @Test
    fun testBatteryLevelEqual() {
        val trigger = TriggerConfig(
            type = TriggerType.BATTERY_LEVEL,
            params = mapOf("level" to "30", "comparison" to "equal")
        )

        assertTrue(evaluator.evaluate(AutomationEvent.BatteryEvent(30, false), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.BatteryEvent(29, false), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.BatteryEvent(31, false), trigger))
    }

    @Test
    fun testBatteryLevelAbove() {
        val trigger = TriggerConfig(
            type = TriggerType.BATTERY_LEVEL,
            params = mapOf("level" to "30", "comparison" to "above")
        )

        assertTrue(evaluator.evaluate(AutomationEvent.BatteryEvent(30, false), trigger))
        assertTrue(evaluator.evaluate(AutomationEvent.BatteryEvent(85, false), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.BatteryEvent(29, false), trigger))
    }

    @Test
    fun testLocationArriveByName() {
        val trigger = TriggerConfig(
            type = TriggerType.LOCATION_ARRIVE,
            params = mapOf("locationName" to "Home")
        )

        assertTrue(evaluator.evaluate(AutomationEvent.LocationEvent("Home", isEntering = true), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.LocationEvent("Home", isEntering = false), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.LocationEvent("Work", isEntering = true), trigger))
    }

    @Test
    fun testLocationLeaveByName() {
        val trigger = TriggerConfig(
            type = TriggerType.LOCATION_LEAVE,
            params = mapOf("locationName" to "Home")
        )

        assertTrue(evaluator.evaluate(AutomationEvent.LocationEvent("Home", isEntering = false), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.LocationEvent("Home", isEntering = true), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.LocationEvent("Office", isEntering = false), trigger))
    }

    @Test
    fun testWifiSpecificNetwork() {
        val trigger = TriggerConfig(
            type = TriggerType.WIFI_SPECIFIC_NETWORK,
            params = mapOf("wifiName" to "OfficeNet")
        )

        assertTrue(evaluator.evaluate(AutomationEvent.WifiEvent(isConnected = true, ssid = "OfficeNet"), trigger))
        assertTrue(evaluator.evaluate(AutomationEvent.WifiEvent(isConnected = true, ssid = "\"OfficeNet\""), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.WifiEvent(isConnected = false, ssid = "OfficeNet"), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.WifiEvent(isConnected = true, ssid = "HomeWifi"), trigger))
    }

    @Test
    fun testTimeRangeDaytime() {
        val trigger = TriggerConfig(
            type = TriggerType.TIME_RANGE,
            params = mapOf("startTime" to "09:00", "endTime" to "17:00")
        )

        assertTrue(evaluator.evaluate(AutomationEvent.TimeEvent(9, 0, 1), trigger))
        assertTrue(evaluator.evaluate(AutomationEvent.TimeEvent(12, 30, 1), trigger))
        assertTrue(evaluator.evaluate(AutomationEvent.TimeEvent(17, 0, 1), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.TimeEvent(8, 59, 1), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.TimeEvent(17, 1, 1), trigger))
    }

    @Test
    fun testTimeRangeOvernight() {
        val trigger = TriggerConfig(
            type = TriggerType.TIME_RANGE,
            params = mapOf("startTime" to "22:00", "endTime" to "06:00")
        )

        assertTrue(evaluator.evaluate(AutomationEvent.TimeEvent(22, 0, 1), trigger))
        assertTrue(evaluator.evaluate(AutomationEvent.TimeEvent(23, 45, 1), trigger))
        assertTrue(evaluator.evaluate(AutomationEvent.TimeEvent(2, 0, 1), trigger))
        assertTrue(evaluator.evaluate(AutomationEvent.TimeEvent(6, 0, 1), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.TimeEvent(21, 59, 1), trigger))
        assertFalse(evaluator.evaluate(AutomationEvent.TimeEvent(6, 1, 1), trigger))
    }
}
