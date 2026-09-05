# MoodyRoutine

> Samsung OneUI-style Modes & Routines — for every Android device.

MoodyRoutine brings the power and polish of Samsung's Modes & Routines app to **any** Android device. Create custom modes and automated routines with a clean, intuitive interface inspired by Samsung's OneUI design language.

## Features

### 🎭 Modes
Create situational profiles that bundle multiple device settings together:
- **Sleep Mode** — Set DND, lower brightness, enable dark mode
- **Work Mode** — Silent ringer, auto-rotate off, specific volume levels
- **Custom Modes** — Create your own with any combination of actions
- Activate manually with one tap, or set auto-triggers

### ⚡ Routines
Automate your device with IF → THEN logic:

**Triggers (IF):**
| Category | Triggers |
|---|---|
| ⏰ Time | Specific time, day of week |
| 🔋 Battery | Level threshold, charging/discharging |
| 📶 Connectivity | Wi-Fi connect/disconnect, specific network |
| 📱 Bluetooth | Device connect/disconnect, specific device |
| 📲 Apps | App opened/closed |
| 🎧 Device | Headphones, screen on/off, power connected |

**Actions (THEN):**
| Category | Actions |
|---|---|
| 🔊 Sound | Ring/media/alarm/notification volume, ringer mode |
| 🌓 Display | Brightness, auto-rotate, dark mode |
| 📵 DND | On/off, priority only, alarms only |
| 📱 Apps | Open app |
| 🔦 Device | Flashlight toggle |
| 🔔 Notification | Send custom notification |

### 🎨 Samsung OneUI-Inspired Design
- Clean, card-based interface
- Intuitive IF → THEN routine creation flow
- Mode color coding and icon customization
- Material 3 with dynamic colors (Android 12+)

## Installation

### Download APK
1. Go to [Releases](../../releases)
2. Download the latest `.apk` file
3. Install on your Android device (enable "Install from unknown sources" if prompted)

### Build from Source
```bash
git clone https://github.com/ndev-hoster/moods-and-routes.git
cd moods-and-routes
./gradlew assembleDebug
```
The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Requirements
- Android 8.0 (API 26) or higher
- Permissions granted as prompted (varies by feature)

## Permissions Used

| Permission | Purpose |
|---|---|
| Foreground Service | Keep automation engine running |
| Do Not Disturb Access | Toggle DND modes |
| Modify System Settings | Change brightness, auto-rotate |
| Notification Access | Read notifications (future) |
| Usage Stats | Detect app open/close |
| Exact Alarms | Time-based triggers |

## Roadmap

- [x] Phase 1: Core modes & routines with basic triggers/actions
- [ ] Phase 2: Geofencing, weather triggers, accessibility service, touch macros
- [ ] Phase 3: Widgets, templates, import/export, Shizuku integration
- [ ] Phase 4: Tasker plugin interop, community routine sharing

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Database:** Room
- **Architecture:** MVVM + Repository pattern
- **Build:** Gradle with Version Catalog

## License
MIT

## Credits
Inspired by Samsung's Modes & Routines from OneUI.
