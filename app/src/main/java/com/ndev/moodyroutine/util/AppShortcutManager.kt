package com.ndev.moodyroutine.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.XmlResourceParser
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import org.xmlpull.v1.XmlPullParser

data class AppActionItem(
    val packageName: String,
    val appName: String,
    val actionName: String,
    val shortcutUri: String? = null,
    val isInteractiveCreator: Boolean = false,
    val creatorComponent: String? = null
)

data class AppGroup(
    val packageName: String,
    val appName: String,
    val actions: List<AppActionItem>
)

object AppShortcutManager {

    fun getAppGroups(context: Context): List<AppGroup> {
        val pm = context.packageManager

        // 1. Get all launchable apps
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.GET_META_DATA)

        // 2. Query apps with ACTION_CREATE_SHORTCUT
        val createShortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        val createShortcutActivities = pm.queryIntentActivities(createShortcutIntent, 0)
        val createShortcutMap = mutableMapOf<String, MutableList<ResolveInfo>>()
        for (info in createShortcutActivities) {
            val pkg = info.activityInfo.packageName
            createShortcutMap.getOrPut(pkg) { mutableListOf() }.add(info)
        }

        val result = mutableListOf<AppGroup>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            val appLabel = resolveInfo.loadLabel(pm).toString()
            val actions = mutableListOf<AppActionItem>()

            // Default: Open the app
            actions.add(
                AppActionItem(
                    packageName = pkg,
                    appName = appLabel,
                    actionName = "Open $appLabel",
                    shortcutUri = null
                )
            )

            // 3. Static shortcuts from manifest
            val staticShortcuts = extractStaticShortcuts(context, pkg, resolveInfo)
            actions.addAll(staticShortcuts)

            // 4. Built-in standard actions for popular packages
            val builtInActions = getBuiltInActions(pkg, appLabel)
            for (builtIn in builtInActions) {
                if (actions.none { it.actionName.equals(builtIn.actionName, ignoreCase = true) }) {
                    actions.add(builtIn)
                }
            }

            // 5. Configurable shortcut creators (e.g. Maps Route, WhatsApp contact, etc.)
            val creators = createShortcutMap[pkg]
            if (creators != null) {
                for (creator in creators) {
                    val label = creator.loadLabel(pm).toString()
                    val compName = "${creator.activityInfo.packageName}/${creator.activityInfo.name}"
                    actions.add(
                        AppActionItem(
                            packageName = pkg,
                            appName = appLabel,
                            actionName = "$label (Configure shortcut)",
                            isInteractiveCreator = true,
                            creatorComponent = compName
                        )
                    )
                }
            }

            result.add(AppGroup(packageName = pkg, appName = appLabel, actions = actions))
        }

        return result.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    private fun extractStaticShortcuts(
        context: Context,
        packageName: String,
        resolveInfo: ResolveInfo
    ): List<AppActionItem> {
        val shortcuts = mutableListOf<AppActionItem>()
        val metaData = resolveInfo.activityInfo.metaData ?: return shortcuts
        if (!metaData.containsKey("android.app.shortcuts")) return shortcuts

        val resId = metaData.getInt("android.app.shortcuts")
        if (resId == 0) return shortcuts

        try {
            val appResources = context.packageManager.getResourcesForApplication(packageName)
            val parser: XmlResourceParser = appResources.getXml(resId)
            var eventType = parser.eventType
            var currentLabel: String? = null
            var currentIntentUri: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "shortcut" -> {
                            currentLabel = null
                            currentIntentUri = null
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)
                                if (attrName == "shortLabel" || attrName == "longLabel") {
                                    val attrRes = parser.getAttributeResourceValue(i, 0)
                                    currentLabel = if (attrRes != 0) {
                                        try { appResources.getString(attrRes) } catch (_: Exception) { parser.getAttributeValue(i) }
                                    } else {
                                        parser.getAttributeValue(i)
                                    }
                                }
                            }
                        }
                        "intent" -> {
                            var action: String? = null
                            var targetPkg: String? = null
                            var targetClass: String? = null
                            var dataUri: String? = null

                            for (i in 0 until parser.attributeCount) {
                                when (parser.getAttributeName(i)) {
                                    "action" -> action = parser.getAttributeValue(i)
                                    "targetPackage" -> targetPkg = parser.getAttributeValue(i)
                                    "targetClass" -> targetClass = parser.getAttributeValue(i)
                                    "data" -> dataUri = parser.getAttributeValue(i)
                                }
                            }

                            val intent = Intent()
                            if (action != null) intent.action = action
                            if (targetPkg != null && targetClass != null) {
                                intent.setClassName(targetPkg, targetClass)
                            } else if (targetPkg != null) {
                                intent.setPackage(targetPkg)
                            }
                            if (dataUri != null) intent.data = Uri.parse(dataUri)
                            currentIntentUri = intent.toUri(Intent.URI_INTENT_SCHEME)
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && parser.name == "shortcut") {
                    if (currentLabel != null && currentIntentUri != null) {
                        shortcuts.add(
                            AppActionItem(
                                packageName = packageName,
                                appName = resolveInfo.loadLabel(context.packageManager).toString(),
                                actionName = currentLabel,
                                shortcutUri = currentIntentUri
                            )
                        )
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Ignore parse failures for non-standard XML formats
        }
        return shortcuts
    }

    private fun getBuiltInActions(pkg: String, appName: String): List<AppActionItem> {
        val list = mutableListOf<AppActionItem>()
        when {
            pkg.contains("camera") -> {
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Take a photo",
                        shortcutUri = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Record a video",
                        shortcutUri = Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
            }
            pkg.contains("deskclock") || pkg.contains("alarm") || pkg.contains("clock") -> {
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Show Alarms",
                        shortcutUri = Intent(AlarmClock.ACTION_SHOW_ALARMS).toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Show Timers",
                        shortcutUri = Intent(AlarmClock.ACTION_SHOW_TIMERS).toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
            }
            pkg.contains("youtube") -> {
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Subscriptions",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/feed/subscriptions")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Trending",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/feed/trending")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
            }
            pkg == "com.apple.android.music" || pkg.contains("apple.android.music") -> {
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Play Music (Resume playback)",
                        shortcutUri = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Listen Now",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("apple-music://music.apple.com/listen-now")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Radio",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("apple-music://music.apple.com/radio")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Browse",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("apple-music://music.apple.com/browse")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Library",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("apple-music://music.apple.com/library")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Search",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("apple-music://music.apple.com/search")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
            }
            pkg.contains("maps") -> {
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Navigate Home",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=Home")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "Navigate to Work",
                        shortcutUri = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=Work")).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
            }
            pkg.contains("chrome") || pkg.contains("browser") -> {
                list.add(
                    AppActionItem(
                        packageName = pkg,
                        appName = appName,
                        actionName = "New Search",
                        shortcutUri = Intent(Intent.ACTION_WEB_SEARCH).apply { setPackage(pkg) }.toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
            }
        }
        return list
    }
}
