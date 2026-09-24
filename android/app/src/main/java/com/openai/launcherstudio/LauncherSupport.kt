package com.openai.launcherstudio

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.LruCache
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class LauncherIconRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val diskCacheDir = File(context.filesDir, "app-icon-cache").apply { mkdirs() }

    suspend fun load(app: AppCatalogEntry): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache[app.key]?.let { return@withContext it }

        val cacheFile = iconCacheFile(app)
        val packageUpdatedAt = packageLastUpdateTime(app.packageName)
        if (cacheFile.exists() && cacheFile.lastModified() >= packageUpdatedAt) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.asImageBitmap()?.also {
                iconCache.put(app.key, it)
                return@withContext it
            }
        }

        val drawable = runCatching {
            launcherApps?.getActivityList(app.packageName, Process.myUserHandle())
                ?.firstOrNull { it.componentName.className == app.activityName }
                ?.getBadgedIcon(0)
        }.getOrNull() ?: runCatching {
            packageManager.getActivityIcon(ComponentName(app.packageName, app.activityName))
        }.getOrNull()

        drawable?.toBitmap()?.let { bitmap ->
            runCatching {
                val temporaryFile = File(diskCacheDir, "${cacheFile.name}.tmp")
                temporaryFile.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                if (!temporaryFile.renameTo(cacheFile)) {
                    temporaryFile.copyTo(cacheFile, overwrite = true)
                    temporaryFile.delete()
                }
            }
            bitmap.asImageBitmap().also { iconCache.put(app.key, it) }
        }
    }

    suspend fun warm(apps: List<AppCatalogEntry>) = withContext(Dispatchers.IO) {
        apps.forEach { load(it) }
    }

    private fun iconCacheFile(app: AppCatalogEntry): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(app.key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return File(diskCacheDir, "$digest.png")
    }

    private fun packageLastUpdateTime(packageName: String): Long {
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).lastUpdateTime
            }
        }.getOrDefault(0L)
    }

    companion object {
        private val iconCache = LruCache<String, ImageBitmap>(256)
    }
}

@Composable
fun rememberAppIcon(repository: LauncherIconRepository, app: AppCatalogEntry?): ImageBitmap? {
    val repo = remember(repository) { repository }
    val iconState = produceState<ImageBitmap?>(initialValue = null, key1 = app?.key) {
        value = app?.let { repo.load(it) }
    }
    return iconState.value
}

private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap
    }

    val width = intrinsicWidth.takeIf { it > 0 } ?: 144
    val height = intrinsicHeight.takeIf { it > 0 } ?: 144
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

fun discoverLaunchableApps(context: Context): List<AppCatalogEntry> {
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    val entries = launcherApps
        ?.getActivityList(null, Process.myUserHandle())
        ?.mapNotNull { info -> info.toCatalogEntry() }
        ?.distinctBy { it.key }
        ?.sortedBy { it.label.lowercase() }
        .orEmpty()

    if (entries.isNotEmpty()) {
        return entries
    }

    val pm = context.packageManager
    val fallbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(fallbackIntent, 0)
        .map {
            AppCatalogEntry(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                activityName = it.activityInfo.name
            )
        }
        .distinctBy { it.key }
        .sortedBy { it.label.lowercase() }
}

private fun LauncherActivityInfo.toCatalogEntry(): AppCatalogEntry? {
    val component = componentName ?: return null
    val rawLabel = label?.toString()?.trim()
    return AppCatalogEntry(
        label = rawLabel?.ifBlank { component.packageName } ?: component.packageName,
        packageName = component.packageName,
        activityName = component.className
    )
}

fun launchApp(context: Context, app: AppCatalogEntry) {
    val component = ComponentName(app.packageName, app.activityName)
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    val activityRect = Rect()
    runCatching {
        if (launcherApps != null) {
            launcherApps.startMainActivity(component, Process.myUserHandle(), activityRect, null)
        } else {
            error("LauncherApps service is unavailable.")
        }
    }.getOrElse {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

fun loadInstalledWidgetProviders(context: Context): List<AppWidgetProviderInfo> {
    val manager = AppWidgetManager.getInstance(context)
    val list = manager.getInstalledProvidersForProfile(Process.myUserHandle())
    return list.sortedBy { it.loadLabel(context.packageManager).toString().lowercase(Locale.getDefault()) }
}

fun widgetPlacementSummaryLabel(info: AppWidgetProviderInfo, grid: GridSpec): String {
    val (sx, sy) = estimateWidgetSpan(info, grid)
    val resizeNote = when (info.resizeMode) {
        AppWidgetProviderInfo.RESIZE_NONE -> ""
        else -> ", resizable"
    }
    return "Occupies ${sx} x ${sy} cells, min ${info.minWidth} x ${info.minHeight} dp$resizeNote"
}

fun estimateWidgetSpan(info: AppWidgetProviderInfo?, grid: GridSpec): Pair<Int, Int> {
    if (info == null) return 2 to 2
    val spanX = when {
        info.minWidth >= 300 -> 4
        info.minWidth >= 220 -> 3
        info.minWidth >= 140 -> 2
        else -> 1
    }.coerceIn(1, grid.cols)
    val spanY = when {
        info.minHeight >= 220 -> 3
        info.minHeight >= 140 -> 2
        else -> 1
    }.coerceIn(1, grid.rows)
    return spanX to spanY
}

fun isWidgetResizable(manager: AppWidgetManager, item: HomeItem): Boolean {
    val widgetId = item.appWidgetId ?: return false
    val info = manager.getAppWidgetInfo(widgetId) ?: return false
    return info.resizeMode != AppWidgetProviderInfo.RESIZE_NONE
}

fun makeWidgetHostView(
    context: Context,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    item: HomeItem
): android.appwidget.AppWidgetHostView? {
    val widgetId = item.appWidgetId ?: return null
    val info = manager.getAppWidgetInfo(widgetId) ?: return null
    return host.createView(context, widgetId, info).apply {
        setAppWidget(widgetId, info)
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = false
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setPadding(0, 0, 0, 0)
        clipToPadding = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
}
