package com.openai.launcherstudio

import java.util.UUID

object HomeItemTypes {
    const val APP = "app"
    const val FOLDER = "folder"
    const val WIDGET = "widget"
    const val SHORTCUT = "shortcut"
}

const val MAX_RECENT_APPS = 15

data class AppCatalogFile(
    val generatedAt: String,
    val apps: List<AppCatalogEntry>
)

data class AppCatalogEntry(
    val label: String,
    val packageName: String,
    val activityName: String
) {
    val key: String
        get() = "$packageName/$activityName"

    val sectionKey: String
        get() = label.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"
}

data class GridSpec(
    var cols: Int = 5,
    var rows: Int = 6
)

data class LayoutConfig(
    var version: Int = 2,
    var exportedAt: String? = null,
    var grid: GridSpec = GridSpec(),
    var dock: MutableList<HomeItem> = mutableListOf(),
    var pages: MutableList<PageLayout> = mutableListOf(PageLayout(index = 0)),
    var launchHistory: MutableList<AppRef> = mutableListOf(),
    var homePageIndex: Int = 0,
    var wallpaperPath: String? = null,
    var isLocked: Boolean = false
)

data class PageLayout(
    var index: Int = 0,
    var title: String? = null,
    var items: MutableList<HomeItem> = mutableListOf()
)

data class HomeItem(
    var id: String = UUID.randomUUID().toString(),
    var type: String = HomeItemTypes.APP,
    var x: Int = 0,
    var y: Int = 0,
    var spanX: Int = 1,
    var spanY: Int = 1,
    var packageName: String? = null,
    var activityName: String? = null,
    var title: String? = null,
    var appRefs: MutableList<AppRef> = mutableListOf(),
    var appWidgetId: Int? = null,
    var widgetProvider: String? = null,
    var shortcutIntentUri: String? = null,
    var shortcutIconPath: String? = null,
    var shortcutId: String? = null
) {
    val key: String
        get() = "$x:$y"

    fun labelOrFallback(appIndex: Map<String, AppCatalogEntry>): String {
        return when (type) {
            HomeItemTypes.FOLDER -> title ?: "Folder"
            HomeItemTypes.WIDGET -> title ?: "Widget"
            HomeItemTypes.SHORTCUT -> title ?: "Shortcut"
            else -> title ?: appIndex["${packageName}/${activityName}"]?.label ?: packageName ?: "Unknown"
        }
    }
}

data class AppRef(
    var packageName: String = "",
    var activityName: String = "",
    var slot: Int = -1
) {
    val key: String
        get() = "$packageName/$activityName"
}

fun LayoutConfig.deepCopy(): LayoutConfig = copy(
    grid = grid.copy(),
    dock = dock.map { it.deepCopy() }.toMutableList(),
    pages = pages.map { it.deepCopy() }.toMutableList(),
    launchHistory = launchHistory.map { it.copy() }.toMutableList()
)

fun PageLayout.deepCopy(): PageLayout = copy(
    items = items.map { it.deepCopy() }.toMutableList()
)

fun HomeItem.deepCopy(): HomeItem = copy(
    appRefs = appRefs.map { it.copy() }.toMutableList()
)
