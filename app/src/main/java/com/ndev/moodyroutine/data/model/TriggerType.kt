package com.ndev.moodyroutine.data.model

enum class TriggerCategory {
    TIME, BATTERY, CONNECTIVITY, APP, DEVICE
}

enum class TriggerType(
    val displayName: String,
    val description: String,
    val iconName: String,
    val category: TriggerCategory
) {
    TIME_OF_DAY("Time of day", "Trigger at a specific time", "Schedule", TriggerCategory.TIME),
    TIME_RANGE("Time range", "Trigger during a time range", "Update", TriggerCategory.TIME),
    DAY_OF_WEEK("Day of week", "Trigger on specific days", "CalendarMonth", TriggerCategory.TIME),
    BATTERY_LEVEL("Battery level", "Trigger at a specific battery level", "BatteryFull", TriggerCategory.BATTERY),
    BATTERY_CHARGING("Battery charging", "Trigger when charging", "BatteryChargingFull", TriggerCategory.BATTERY),
    BATTERY_DISCHARGING("Battery discharging", "Trigger when unplugged", "BatteryAlert", TriggerCategory.BATTERY),
    WIFI_CONNECTED("Wi-Fi connected", "Trigger when connected to any Wi-Fi", "Wifi", TriggerCategory.CONNECTIVITY),
    WIFI_DISCONNECTED("Wi-Fi disconnected", "Trigger when disconnected from Wi-Fi", "WifiOff", TriggerCategory.CONNECTIVITY),
    WIFI_SPECIFIC_NETWORK("Specific Wi-Fi", "Trigger when connected to a specific network", "NetworkWifi", TriggerCategory.CONNECTIVITY),
    BLUETOOTH_CONNECTED("Bluetooth connected", "Trigger when connected to any Bluetooth device", "Bluetooth", TriggerCategory.CONNECTIVITY),
    BLUETOOTH_DISCONNECTED("Bluetooth disconnected", "Trigger when disconnected from Bluetooth", "BluetoothDisabled", TriggerCategory.CONNECTIVITY),
    BLUETOOTH_SPECIFIC_DEVICE("Specific Bluetooth device", "Trigger when connected to a specific device", "BluetoothConnected", TriggerCategory.CONNECTIVITY),
    APP_OPENED("App opened", "Trigger when a specific app is opened", "Apps", TriggerCategory.APP),
    APP_CLOSED("App closed", "Trigger when a specific app is closed", "ExitToApp", TriggerCategory.APP),
    HEADPHONE_CONNECTED("Headphones connected", "Trigger when headphones are connected", "Headphones", TriggerCategory.DEVICE),
    HEADPHONE_DISCONNECTED("Headphones disconnected", "Trigger when headphones are disconnected", "HeadphonesBattery", TriggerCategory.DEVICE),
    SCREEN_ON("Screen on", "Trigger when screen turns on", "Smartphone", TriggerCategory.DEVICE),
    SCREEN_OFF("Screen off", "Trigger when screen turns off", "PhoneAndroid", TriggerCategory.DEVICE),
    POWER_CONNECTED("Power connected", "Trigger when power is connected", "Power", TriggerCategory.DEVICE),
    POWER_DISCONNECTED("Power disconnected", "Trigger when power is disconnected", "PowerOff", TriggerCategory.DEVICE)
}
