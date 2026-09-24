package com.openai.launcherstudio

import android.appwidget.AppWidgetManager
import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import java.io.File
import java.time.Instant

private const val MAX_DOCK_SLOTS = 5

class LayoutRepository(private val context: Context) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val layoutFile = File(context.filesDir, "launcher-backup.json")
    private val appCatalogFile = File(context.filesDir, "app-catalog-cache.json")

    fun loadCachedAppCatalog(): List<AppCatalogEntry> {
        if (!appCatalogFile.exists()) return emptyList()
        return runCatching {
            appCatalogFile.readText()
                .takeIf { it.isNotBlank() }
                ?.let { gson.fromJson(it, AppCatalogFile::class.java) }
                ?.apps
                .orEmpty()
                .filter { it.packageName.isNotBlank() && it.activityName.isNotBlank() }
                .distinctBy { it.key }
                .sortedBy { it.label.lowercase() }
        }.getOrDefault(emptyList())
    }

    fun saveAppCatalogCache(apps: List<AppCatalogEntry>) {
        val catalog = AppCatalogFile(
            generatedAt = Instant.now().toString(),
            apps = apps.distinctBy { it.key }.sortedBy { it.label.lowercase() }
        )
        val temporaryFile = File(context.filesDir, "${appCatalogFile.name}.tmp")
        temporaryFile.writeText(gson.toJson(catalog))
        if (!temporaryFile.renameTo(appCatalogFile)) {
            appCatalogFile.writeText(temporaryFile.readText())
            temporaryFile.delete()
        }
    }

    fun loadLayout(installedApps: List<AppCatalogEntry>): LayoutConfig {
        if (!layoutFile.exists()) {
            val defaultLayout = createDefaultLayout(installedApps)
            saveLayout(defaultLayout)
            return defaultLayout
        }

        val parsed = layoutFile.readText().takeIf { it.isNotBlank() }?.let {
            gson.fromJson(it, LayoutConfig::class.java)
        }

        val normalized = normalizeLayout(parsed ?: createDefaultLayout(installedApps))
        if (normalized != parsed) {
            saveLayout(normalized)
        }
        return normalized
    }

    fun loadSavedLayoutOrNull(): LayoutConfig? {
        if (!layoutFile.exists()) return null
        val parsed = layoutFile.readText().takeIf { it.isNotBlank() }?.let {
            gson.fromJson(it, LayoutConfig::class.java)
        } ?: return null
        return normalizeLayout(parsed)
    }

    fun saveLayout(layout: LayoutConfig) {
        val normalized = normalizeLayout(layout).copy(exportedAt = Instant.now().toString())
        layoutFile.writeText(gson.toJson(normalized))
    }

    fun importLayout(uri: Uri): LayoutConfig {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Unable to read backup file.")
        val parsed = gson.fromJson(json, LayoutConfig::class.java)
            ?: error("Backup file is empty.")
        val normalized = normalizeLayout(parsed)
        saveLayout(normalized)
        return normalized
    }

    fun importOriginalLayout(): LayoutConfig {
        if (context.packageName == ORIGINAL_LAUNCHER_PACKAGE) {
            error("The standard launcher cannot apply its own exported layout.")
        }
        val uri = Uri.parse(
            "content://$ORIGINAL_LAYOUT_PROVIDER_AUTHORITY/$ORIGINAL_LAYOUT_FILE_NAME"
        )
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Original launcher layout is unavailable. Open the standard version once first.")
        val parsed = gson.fromJson(json, LayoutConfig::class.java)
            ?: error("Original launcher layout is empty.")
        val normalized = normalizeLayout(parsed)
        saveLayout(normalized)
        return normalized
    }

    fun exportLayout(uri: Uri, layout: LayoutConfig) {
        val normalized = normalizeLayout(layout).copy(exportedAt = Instant.now().toString())
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
            it.write(gson.toJson(normalized))
        } ?: error("Unable to open export location.")
    }

    fun exportAppCatalog(uri: Uri, apps: List<AppCatalogEntry>) {
        val file = AppCatalogFile(
            generatedAt = Instant.now().toString(),
            apps = apps.sortedBy { it.label.lowercase() }
        )
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
            it.write(gson.toJson(file))
        } ?: error("Unable to open export location.")
    }

    fun recordLaunch(layout: LayoutConfig, app: AppCatalogEntry): LayoutConfig {
        val copy = normalizeLayout(layout).deepCopy()
        copy.launchHistory.removeAll { it.key == app.key }
        copy.launchHistory.add(0, AppRef(packageName = app.packageName, activityName = app.activityName))
        copy.launchHistory = copy.launchHistory.take(MAX_RECENT_APPS).toMutableList()
        saveLayout(copy)
        return copy
    }

    fun addInstalledPackage(packageName: String): LayoutConfig? {
        val app = discoverLaunchableApps(context)
            .firstOrNull { it.packageName == packageName }
            ?: return null
        val current = loadLayout(discoverLaunchableApps(context)).deepCopy()
        val alreadyPresent = current.dock.any { it.matchesApp(app) } ||
            current.pages.any { page -> page.items.any { it.matchesApp(app) } }
        if (alreadyPresent) return current

        var placement = findFirstAvailableCell(current, current.homePageIndex)
        if (placement == null) {
            val newPageIndex = current.pages.size
            current.pages.add(PageLayout(index = newPageIndex))
            placement = ShortcutPlacement(newPageIndex, 0, 0)
        }
        current.pages[placement.pageIndex].items.add(
            HomeItem(
                type = HomeItemTypes.APP,
                x = placement.x,
                y = placement.y,
                packageName = app.packageName,
                activityName = app.activityName
            )
        )
        saveLayout(current)
        return current
    }

    fun addShortcut(shortcut: PendingShortcut, preferredPage: Int? = null): LayoutConfig? {
        val current = loadLayout(discoverLaunchableApps(context)).deepCopy()
        val duplicateExists = current.pages.any { page ->
            page.items.any { item ->
                item.type == HomeItemTypes.SHORTCUT &&
                    (
                        (!shortcut.shortcutId.isNullOrBlank() && item.shortcutId == shortcut.shortcutId && item.packageName == shortcut.packageName) ||
                            item.shortcutIntentUri == shortcut.intentUri
                    )
            }
        }
        if (duplicateExists) return current

        val placement = findFirstAvailableCell(
            layout = current,
            preferredPage = preferredPage ?: current.homePageIndex
        ) ?: return null
        current.pages[placement.pageIndex].items.add(
            HomeItem(
                type = HomeItemTypes.SHORTCUT,
                x = placement.x,
                y = placement.y,
                spanX = 1,
                spanY = 1,
                title = shortcut.label,
                packageName = shortcut.packageName,
                shortcutIntentUri = shortcut.intentUri,
                shortcutIconPath = shortcut.iconPath,
                shortcutId = shortcut.shortcutId
            )
        )
        saveLayout(current)
        return current
    }

    private fun normalizeLayout(layout: LayoutConfig): LayoutConfig {
        val manager = AppWidgetManager.getInstance(context)
        val safeCols = layout.grid.cols.coerceIn(3, 8)
        val safeRows = layout.grid.rows.coerceIn(4, 8)

        val normalizedDock = mutableListOf<HomeItem>()
        val occupiedDockSlots = mutableSetOf<Int>()
        layout.dock
            .sortedBy { it.x }
            .forEach { item ->
                val normalizedItem = normalizeItem(
                    item = item,
                    maxCols = MAX_DOCK_SLOTS,
                    maxRows = 1,
                    forcedY = 0,
                    widgetValidator = manager::getAppWidgetInfo
                ) ?: return@forEach

                var slot = normalizedItem.x.coerceIn(0, MAX_DOCK_SLOTS - 1)
                if (slot in occupiedDockSlots) {
                    slot = (0 until MAX_DOCK_SLOTS).firstOrNull { it !in occupiedDockSlots } ?: return@forEach
                }

                normalizedItem.x = slot
                occupiedDockSlots += slot
                normalizedDock += normalizedItem
            }

        val normalizedPages = layout.pages
            .ifEmpty { mutableListOf(PageLayout(index = 0)) }
            .sortedBy { it.index }
            .mapIndexed { pageIndex, page ->
                val occupied = mutableSetOf<String>()
                val items = page.items.mapNotNull { item ->
                    normalizeItem(
                        item = item,
                        maxCols = safeCols,
                        maxRows = safeRows,
                        widgetValidator = manager::getAppWidgetInfo
                    )?.takeIf {
                        val cells = occupiedCells(it)
                        if (cells.any(occupied::contains)) {
                            false
                        } else {
                            occupied += cells
                            true
                        }
                    }
                }.toMutableList()

                PageLayout(
                    index = pageIndex,
                    title = page.title?.trim()?.ifBlank { null },
                    items = items
                )
            }
            .toMutableList()

        val pageList = normalizedPages.ifEmpty { mutableListOf(PageLayout(index = 0)) }
        return LayoutConfig(
            version = layout.version.coerceAtLeast(2),
            exportedAt = layout.exportedAt,
            grid = GridSpec(cols = safeCols, rows = safeRows),
            dock = normalizedDock,
            pages = pageList,
            launchHistory = layout.launchHistory
                .filter { it.packageName.isNotBlank() && it.activityName.isNotBlank() }
                .distinctBy { it.key }
                .take(MAX_RECENT_APPS)
                .toMutableList(),
            homePageIndex = layout.homePageIndex.coerceIn(0, pageList.lastIndex.coerceAtLeast(0)),
            wallpaperPath = layout.wallpaperPath?.takeIf { File(it).exists() },
            isLocked = layout.isLocked
        )
    }

    fun saveWallpaper(uri: Uri): String {
        context.filesDir.listFiles()?.forEach {
            if (it.name.startsWith("wallpaper-")) it.delete()
        }
        val target = File(context.filesDir, "wallpaper-${Instant.now().toEpochMilli()}.dat")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to read wallpaper image.")
        return target.absolutePath
    }

    fun clearWallpaper() {
        context.filesDir.listFiles()?.forEach {
            if (it.name.startsWith("wallpaper-")) it.delete()
        }
    }

    private fun normalizeItem(
        item: HomeItem,
        maxCols: Int,
        maxRows: Int,
        forcedX: Int? = null,
        forcedY: Int? = null,
        widgetValidator: (Int) -> Any?
    ): HomeItem? {
        val spanX = item.spanX.coerceIn(1, maxCols)
        val spanY = item.spanY.coerceIn(1, maxRows)
        val x = (forcedX ?: item.x).coerceIn(0, maxCols - spanX)
        val y = (forcedY ?: item.y).coerceIn(0, maxRows - spanY)
        val normalized = item.deepCopy().copy(
            id = item.id.ifBlank { java.util.UUID.randomUUID().toString() },
            type = when (item.type) {
                HomeItemTypes.FOLDER, HomeItemTypes.WIDGET, HomeItemTypes.SHORTCUT -> item.type
                else -> HomeItemTypes.APP
            },
            x = x,
            y = y,
            spanX = spanX,
            spanY = spanY,
            title = item.title?.trim()?.ifBlank { null }
        )

        return when (normalized.type) {
            HomeItemTypes.APP -> {
                if (normalized.packageName.isNullOrBlank() || normalized.activityName.isNullOrBlank()) {
                    null
                } else {
                    normalized.copy(appRefs = mutableListOf(), appWidgetId = null, widgetProvider = null)
                }
            }

            HomeItemTypes.FOLDER -> {
                val refs = normalized.appRefs
                    .filter { it.packageName.isNotBlank() && it.activityName.isNotBlank() }
                    .distinctBy { it.key }
                    .toMutableList()
                if (refs.isEmpty()) {
                    null
                } else {
                    normalized.copy(appRefs = refs, packageName = null, activityName = null, appWidgetId = null, widgetProvider = null)
                }
            }

            HomeItemTypes.WIDGET -> {
                val widgetId = normalized.appWidgetId ?: return null
                if (widgetValidator(widgetId) == null) {
                    null
                } else {
                    normalized.copy(
                        packageName = null,
                        activityName = null,
                        appRefs = mutableListOf()
                    )
                }
            }

            HomeItemTypes.SHORTCUT -> {
                if (normalized.shortcutIntentUri.isNullOrBlank()) {
                    null
                } else {
                    normalized.copy(
                        appRefs = mutableListOf(),
                        appWidgetId = null,
                        widgetProvider = null,
                        activityName = null
                    )
                }
            }

            else -> null
        }
    }

    private fun createDefaultLayout(installedApps: List<AppCatalogEntry>): LayoutConfig {
        val grid = GridSpec(cols = 5, rows = 6)
        val sortedApps = installedApps.sortedBy { it.label.lowercase() }
        val dockApps = sortedApps.take(4).mapIndexed { index, app ->
            HomeItem(
                type = HomeItemTypes.APP,
                x = index,
                y = 0,
                packageName = app.packageName,
                activityName = app.activityName
            )
        }.toMutableList()

        val pageSize = grid.cols * grid.rows
        val pages = sortedApps.drop(dockApps.size).chunked(pageSize).mapIndexed { pageIndex, chunk ->
            PageLayout(
                index = pageIndex,
                items = chunk.mapIndexed { itemIndex, app ->
                    HomeItem(
                        type = HomeItemTypes.APP,
                        x = itemIndex % grid.cols,
                        y = itemIndex / grid.cols,
                        packageName = app.packageName,
                        activityName = app.activityName
                    )
                }.toMutableList()
            )
        }.toMutableList()

        if (pages.isEmpty()) {
            pages += PageLayout(index = 0)
        }

        return LayoutConfig(
            grid = grid,
            dock = dockApps,
            pages = pages
        )
    }
}

private fun HomeItem.matchesApp(app: AppCatalogEntry): Boolean {
    if (type == HomeItemTypes.APP && packageName == app.packageName && activityName == app.activityName) return true
    return type == HomeItemTypes.FOLDER && appRefs.any { it.key == app.key }
}

private data class ShortcutPlacement(val pageIndex: Int, val x: Int, val y: Int)

private fun findFirstAvailableCell(layout: LayoutConfig, preferredPage: Int): ShortcutPlacement? {
    val safePreferred = preferredPage.coerceIn(0, layout.pages.lastIndex.coerceAtLeast(0))
    val pageOrder = (listOf(safePreferred) + layout.pages.indices.filter { it != safePreferred }).distinct()
    pageOrder.forEach { pageIndex ->
        val page = layout.pages.getOrNull(pageIndex) ?: return@forEach
        for (y in 0 until layout.grid.rows) {
            for (x in 0 until layout.grid.cols) {
                val occupied = page.items.any { item ->
                    x < item.x + item.spanX &&
                        x + 1 > item.x &&
                        y < item.y + item.spanY &&
                        y + 1 > item.y
                }
                if (!occupied) return ShortcutPlacement(pageIndex, x, y)
            }
        }
    }
    return null
}

private fun occupiedCells(item: HomeItem): List<String> {
    val cells = mutableListOf<String>()
    repeat(item.spanX) { dx ->
        repeat(item.spanY) { dy ->
            cells += "${item.x + dx}:${item.y + dy}"
        }
    }
    return cells
}
