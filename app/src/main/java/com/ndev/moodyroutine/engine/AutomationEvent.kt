package com.ndev.moodyroutine.engine

sealed class AutomationEvent {
    data class TimeEvent(val hour: Int, val minute: Int, val dayOfWeek: Int) : AutomationEvent()
    data class BatteryEvent(val level: Int, val isCharging: Boolean) : AutomationEvent()
    data class WifiEvent(val isConnected: Boolean, val ssid: String?) : AutomationEvent()
    data class BluetoothEvent(val isConnected: Boolean, val deviceName: String?, val deviceAddress: String?) : AutomationEvent()
    data class AppEvent(val packageName: String, val isOpened: Boolean) : AutomationEvent()
    data class HeadphoneEvent(val isConnected: Boolean) : AutomationEvent()
    data class ScreenEvent(val isOn: Boolean) : AutomationEvent()
    data class PowerEvent(val isConnected: Boolean) : AutomationEvent()
}
