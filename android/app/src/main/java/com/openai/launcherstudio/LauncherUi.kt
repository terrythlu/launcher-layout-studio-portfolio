@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.openai.launcherstudio

import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.roundToInt

private const val DOCK_SLOTS = 5
private const val FOLDER_GRID_COLS = 4
private const val FOLDER_GRID_ROWS = 4
private const val FOLDER_PAGE_SIZE = FOLDER_GRID_COLS * FOLDER_GRID_ROWS
private const val FOLDER_PREVIEW_SIZE = 4

private data class FolderSlotAssignment(
    val ref: AppRef,
    val app: AppCatalogEntry,
    val slot: Int
)

private data class FolderSlotInfo(
    val pageCount: Int,
    val assignments: List<FolderSlotAssignment>
)

private data class FolderOpenAnchor(
    val centerYInWindowPx: Float,
    val sourceHeightPx: Float
)

private fun resolveFolderApps(
    item: HomeItem,
    appIndex: Map<String, AppCatalogEntry>
): List<Pair<AppRef, AppCatalogEntry>> {
    // Resolve the complete reference list before limiting the preview. Otherwise
    // one stale entry in the first four can hide a valid app that follows it.
    return item.appRefs.mapNotNull { ref ->
        appIndex[ref.key]?.let { app -> ref to app }
    }
}

private fun computeFolderSlots(
    refs: List<Pair<AppRef, AppCatalogEntry>>,
    pageSize: Int
): FolderSlotInfo {
    val occupied = mutableSetOf<Int>()
    val assignments = mutableListOf<FolderSlotAssignment>()
    refs.filter { it.first.slot >= 0 }.forEach { (ref, app) ->
        var s = ref.slot
        while (s in occupied) s++
        occupied.add(s)
        assignments.add(FolderSlotAssignment(ref, app, s))
    }
    var nextSlot = 0
    refs.filter { it.first.slot < 0 }.forEach { (ref, app) ->
        while (nextSlot in occupied) nextSlot++
        occupied.add(nextSlot)
        assignments.add(FolderSlotAssignment(ref, app, nextSlot))
        nextSlot++
    }
    val maxSlot = assignments.maxOfOrNull { it.slot } ?: -1
    val pageCount = (maxSlot / pageSize + 1).coerceAtLeast(1)
    return FolderSlotInfo(pageCount, assignments.sortedBy { it.slot })
}

private fun folderPreviewApps(
    item: HomeItem,
    appIndex: Map<String, AppCatalogEntry>
): List<AppCatalogEntry?> {
    return resolveFolderApps(item, appIndex)
        .take(FOLDER_PREVIEW_SIZE)
        .map { it.second }
}

private data class DockLayoutInfo(
    val originXPx: Float,
    val originYPx: Float,
    val slotStepPx: Float,
    val slotCount: Int
) {
    fun slotAt(screenX: Float): Int? {
        if (slotStepPx <= 0f || slotCount <= 0) return null
        val relX = screenX - originXPx
        return (relX / slotStepPx).toInt().coerceIn(0, slotCount - 1)
    }
}

private data class HomeGridInfo(
    val pageIndex: Int,
    val originXPx: Float,
    val originYPx: Float,
    val cellStepXPx: Float,
    val cellStepYPx: Float,
    val cols: Int,
    val rows: Int
) {
    fun screenToCell(screenX: Float, screenY: Float): Pair<Int, Int>? {
        val relX = screenX - originXPx
        val relY = screenY - originYPx
        if (cellStepXPx <= 0f || cellStepYPx <= 0f) return null
        val col = (relX / cellStepXPx).toInt()
        val row = (relY / cellStepYPx).toInt()
        if (col !in 0 until cols || row !in 0 until rows) return null
        return col to row
    }
}

private suspend fun PointerInputScope.detectTapOrLongPressDrag(
    onTap: () -> Unit,
    onLongPressStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = viewConfiguration.touchSlop
        var timedOut = false
        var movedBeyondSlop = false
        var upChange: PointerInputChange? = null

        try {
            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                        ?: return@withTimeout
                    if (change.changedToUp()) {
                        upChange = change
                        return@withTimeout
                    }
                    if (change.isConsumed) {
                        return@withTimeout
                    }
                    val totalDelta = (change.position - down.position).getDistance()
                    if (totalDelta > slop) {
                        movedBeyondSlop = true
                        return@withTimeout
                    }
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            timedOut = true
        }

        when {
            upChange != null && !movedBeyondSlop -> {
                upChange?.consume()
                onTap()
            }
            timedOut && !movedBeyondSlop -> {
                onLongPressStart(down.position)
                var dragSucceeded = false
                try {
                    dragSucceeded = drag(down.id) { change ->
                        val delta = change.positionChange()
                        onDrag(change, delta)
                        change.consume()
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                    onDragCancel()
                    throw kotlinx.coroutines.CancellationException()
                }
                if (dragSucceeded) onDragEnd() else onDragCancel()
            }
        }
    }
}

private data class ItemTarget(
    val area: Area,
    val pageIndex: Int? = null,
    val itemId: String
)

private data class PageDragGhost(
    val item: HomeItem,
    val centerInWindow: Offset,
    val widthPx: Float,
    val heightPx: Float
)

private data class FolderAppMenuTarget(
    val folderId: String,
    val ref: AppRef,
    val app: AppCatalogEntry
)

private data class EmptyTarget(
    val pageIndex: Int,
    val x: Int,
    val y: Int
)

private data class WidgetSizeTarget(
    val itemTarget: ItemTarget,
    val spanX: Int,
    val spanY: Int
)

private enum class Area { PAGE, DOCK }

private fun runWidgetPostBindAfterPick(
    widgetId: Int,
    layout: LayoutConfig,
    pendingPlacement: EmptyTarget?,
    appWidgetManager: AppWidgetManager,
    repository: LayoutRepository,
    activity: MainActivity,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onLayoutCommitted: (LayoutConfig) -> Unit,
    launchConfigure: (Intent) -> Unit,
    setWidgetConfigureId: (Int) -> Unit,
    clearPendingIfCommitted: () -> Unit
) {
    val info = appWidgetManager.getAppWidgetInfo(widgetId)
    if (info?.configure != null) {
        setWidgetConfigureId(widgetId)
        val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = info.configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        launchConfigure(configureIntent)
        return
    }
    commitWidget(layout, widgetId, pendingPlacement, appWidgetManager, repository)?.also {
        onLayoutCommitted(it)
        scope.launch { snackbarHostState.showSnackbar("Widget added") }
    } ?: run {
        activity.appWidgetHost.deleteAppWidgetId(widgetId)
        scope.launch { snackbarHostState.showSnackbar("No room for that widget") }
    }
    clearPendingIfCommitted()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherRoot(activity: MainActivity) {
    val context = activity.applicationContext
    val canApplyOriginalLayout = context.packageName != ORIGINAL_LAUNCHER_PACKAGE
    val isParallelVariant = context.packageName == "$ORIGINAL_LAUNCHER_PACKAGE.parallel"
    val repository = remember(context) { LayoutRepository(context) }
    val iconRepository = remember(context) { LauncherIconRepository(context) }
    val roleManager = remember(context) { context.getSystemService(RoleManager::class.java) }
    val appWidgetManager = remember(activity) { AppWidgetManager.getInstance(activity) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var installedApps by remember { mutableStateOf<List<AppCatalogEntry>>(emptyList()) }
    var layout by remember { mutableStateOf(LayoutConfig()) }
    var refreshSeed by rememberSaveable { mutableIntStateOf(0) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var drawerVisible by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by rememberSaveable { mutableStateOf(false) }
    var folderItem by remember { mutableStateOf<HomeItem?>(null) }
    var folderOpenAnchor by remember { mutableStateOf<FolderOpenAnchor?>(null) }
    var folderAppMenuTarget by remember { mutableStateOf<FolderAppMenuTarget?>(null) }
    var homeGridInfo by remember { mutableStateOf<HomeGridInfo?>(null) }
    val homeGridInfos = remember { mutableStateMapOf<Int, HomeGridInfo>() }
    var dockLayoutInfo by remember { mutableStateOf<DockLayoutInfo?>(null) }
    var selectedItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var peekTarget by remember { mutableStateOf<ItemTarget?>(null) }
    var contextMenuTarget by remember { mutableStateOf<ItemTarget?>(null) }
    var renameTarget by remember { mutableStateOf<ItemTarget?>(null) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    var widgetSizeTarget by remember { mutableStateOf<WidgetSizeTarget?>(null) }
    var selectedEmptyTarget by remember { mutableStateOf<EmptyTarget?>(null) }
    var pendingWidgetPlacement by remember { mutableStateOf<EmptyTarget?>(null) }
    var pendingWidgetId by rememberSaveable { mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }
    var widgetConfigureId by rememberSaveable { mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }
    var widgetPickerVisible by remember { mutableStateOf(false) }
    var widgetProviders by remember { mutableStateOf<List<AppWidgetProviderInfo>>(emptyList()) }
    var widgetListLoading by remember { mutableStateOf(false) }
    var deleteDropBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var deleteDropVisible by remember { mutableStateOf(false) }
    var deleteDropHovered by remember { mutableStateOf(false) }
    var recentAppsVisible by rememberSaveable { mutableStateOf(false) }
    var recentAppsStripPressed by remember { mutableStateOf(false) }
    var pageDragGhost by remember { mutableStateOf<PageDragGhost?>(null) }
    var rootOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var homeRoleRequestFallbackPending by rememberSaveable { mutableStateOf(false) }
    var handledAutoAction by rememberSaveable { mutableStateOf<String?>(null) }
    var floatingButtonEnabled by remember { mutableStateOf(FloatingButtonService.isEnabled(context)) }
    var floatingAlpha by remember { mutableStateOf(FloatingButtonService.getAlpha(context)) }
    var floatingOuterDp by remember { mutableStateOf(FloatingButtonService.getOuterSize(context)) }
    var floatingIconDp by remember { mutableStateOf(FloatingButtonService.getIconSize(context)) }
    var floatingFlingTrigger by remember { mutableStateOf(FloatingButtonService.getFlingTriggerVelocity(context)) }
    var isBootstrapLoading by rememberSaveable { mutableStateOf(true) }
    var userInteractedDuringBootstrap by rememberSaveable { mutableStateOf(false) }

    val exportAppsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching { repository.exportAppCatalog(uri, installedApps) }
                .onSuccess { scope.launch { snackbarHostState.showSnackbar("Saved apps.json") } }
                .onFailure { scope.launch { snackbarHostState.showSnackbar(it.message ?: "Export failed") } }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching { repository.exportLayout(uri, layout) }
                .onSuccess { scope.launch { snackbarHostState.showSnackbar("Saved launcher backup") } }
                .onFailure { scope.launch { snackbarHostState.showSnackbar(it.message ?: "Export failed") } }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { layout = repository.importLayout(uri) }
                .onSuccess {
                    selectedEmptyTarget = null
                    scope.launch { snackbarHostState.showSnackbar("Backup restored") }
                }
                .onFailure { scope.launch { snackbarHostState.showSnackbar(it.message ?: "Import failed") } }
        }
    }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val savedPath = repository.saveWallpaper(uri)
                val updated = layout.deepCopy().also { it.wallpaperPath = savedPath }
                layout = updated
                repository.saveLayout(updated)
            }
                .onSuccess { scope.launch { snackbarHostState.showSnackbar("Wallpaper updated") } }
                .onFailure { scope.launch { snackbarHostState.showSnackbar(it.message ?: "Wallpaper failed") } }
        }
    }

    val widgetConfigureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val widgetId = widgetConfigureId
        widgetConfigureId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (result.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            commitWidget(layout, widgetId, pendingWidgetPlacement, appWidgetManager, repository)?.also {
                layout = it
                scope.launch { snackbarHostState.showSnackbar("Widget added") }
            } ?: run {
                activity.appWidgetHost.deleteAppWidgetId(widgetId)
                scope.launch { snackbarHostState.showSnackbar("No room for that widget") }
            }
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            activity.appWidgetHost.deleteAppWidgetId(widgetId)
        }
        pendingWidgetPlacement = null
    }

    val widgetPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        if (result.resultCode != Activity.RESULT_OK || widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                activity.appWidgetHost.deleteAppWidgetId(widgetId)
            }
            pendingWidgetPlacement = null
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            return@rememberLauncherForActivityResult
        }
        runWidgetPostBindAfterPick(
            widgetId = widgetId,
            layout = layout,
            pendingPlacement = pendingWidgetPlacement,
            appWidgetManager = appWidgetManager,
            repository = repository,
            activity = activity,
            scope = scope,
            snackbarHostState = snackbarHostState,
            onLayoutCommitted = { layout = it },
            launchConfigure = { widgetConfigureLauncher.launch(it) },
            setWidgetConfigureId = { widgetConfigureId = it },
            clearPendingIfCommitted = { pendingWidgetPlacement = null }
        )
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    fun launchHomeSettingsScreen() {
        val intents = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        val targetIntent = intents.firstOrNull { it.resolveActivity(context.packageManager) != null }
        if (targetIntent != null) {
            context.startActivity(targetIntent)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Home app settings are not available on this device.") }
        }
    }

    val homeRoleRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val isHeld = roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        if (isHeld) {
            scope.launch { snackbarHostState.showSnackbar("Launcher Layout Studio is now the default home app.") }
        } else if (homeRoleRequestFallbackPending) {
            launchHomeSettingsScreen()
        } else {
            scope.launch { snackbarHostState.showSnackbar("Home app request was not granted.") }
        }
        homeRoleRequestFallbackPending = false
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (FloatingButtonService.canShowOverlay(context)) {
            FloatingButtonService.setEnabled(context, true)
            floatingButtonEnabled = true
            scope.launch { snackbarHostState.showSnackbar("Floating button enabled. Tap it from any screen to open this app.") }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Overlay permission not granted.") }
        }
    }

    fun toggleFloatingButton() {
        if (floatingButtonEnabled) {
            FloatingButtonService.setEnabled(context, false)
            floatingButtonEnabled = false
            scope.launch { snackbarHostState.showSnackbar("Floating button disabled.") }
            return
        }
        if (FloatingButtonService.canShowOverlay(context)) {
            FloatingButtonService.setEnabled(context, true)
            floatingButtonEnabled = true
            scope.launch { snackbarHostState.showSnackbar("Floating button enabled. Tap it from any screen to open this app.") }
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { overlayPermissionLauncher.launch(intent) }
                .onFailure {
                    scope.launch { snackbarHostState.showSnackbar("Cannot open overlay permission settings.") }
                }
        }
    }

    LaunchedEffect(refreshSeed) {
        isBootstrapLoading = true
        userInteractedDuringBootstrap = false
        val (quickLayout, cachedApps) = withContext(Dispatchers.IO) {
            repository.loadSavedLayoutOrNull() to repository.loadCachedAppCatalog()
        }
        if (quickLayout != null) {
            layout = quickLayout
        }
        if (cachedApps.isNotEmpty()) {
            installedApps = cachedApps
        }

        val apps = withContext(Dispatchers.IO) {
            discoverLaunchableApps(context)
        }
        installedApps = apps
        withContext(Dispatchers.IO) {
            repository.saveAppCatalogCache(apps)
        }

        val loaded = withContext(Dispatchers.IO) {
            repository.loadLayout(apps)
        }
        if (!userInteractedDuringBootstrap && (quickLayout == null || layout == quickLayout)) {
            layout = loaded
        }
        isBootstrapLoading = false
        scope.launch(Dispatchers.IO) {
            iconRepository.warm(apps)
        }
    }

    LaunchedEffect(layout) {
        val currentFolder = folderItem
        if (currentFolder != null) {
            val fresh = layout.findFolderById(currentFolder.id)
            if (fresh == null) {
                folderItem = null
                folderOpenAnchor = null
            } else if (fresh.appRefs != currentFolder.appRefs || fresh.title != currentFolder.title) {
                folderItem = fresh.deepCopy()
            }
        }
    }

    val appIndex = remember(installedApps) { installedApps.associateBy { it.key } }
    val recentApps = remember(layout, appIndex) {
        layout.launchHistory.mapNotNull { appIndex[it.key] }.take(MAX_RECENT_APPS)
    }
    val dockRecentApps = remember(recentApps) { recentApps.asReversed() }
    val pagerState = rememberPagerState(pageCount = { layout.pages.size.coerceAtLeast(1) })
    val wallpaperBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = layout.wallpaperPath) {
        val path = layout.wallpaperPath
        value = if (path.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    val homePageIndex = layout.homePageIndex.coerceIn(0, layout.pages.lastIndex.coerceAtLeast(0))

    LaunchedEffect(widgetPickerVisible) {
        if (widgetPickerVisible) {
            widgetListLoading = true
            widgetProviders = withContext(Dispatchers.IO) {
                loadInstalledWidgetProviders(context)
            }
            widgetListLoading = false
        }
    }

    fun commitLayout(newLayout: LayoutConfig) {
        if (isBootstrapLoading) {
            userInteractedDuringBootstrap = true
        }
        layout = newLayout
        repository.saveLayout(newLayout)
    }

    fun withUpdatedLayout(block: (LayoutConfig) -> LayoutConfig) {
        commitLayout(block(layout.deepCopy()))
    }

    fun enterEditMode() {
        if (editMode) return
        controlsVisible = false
        drawerVisible = false
        folderItem = null
        folderOpenAnchor = null
        contextMenuTarget = null
        renameTarget = null
        selectedEmptyTarget = null
        selectedItemIds = emptySet()
        editMode = true
    }

    fun navigateToHomePage() {
        val targetHomePage = layout.homePageIndex.coerceIn(0, layout.pages.lastIndex.coerceAtLeast(0))
        if (pagerState.currentPage != targetHomePage) {
            scope.launch { pagerState.animateScrollToPage(targetHomePage) }
        }
    }

    BackHandler(enabled = true) {
        when {
            recentAppsVisible -> recentAppsVisible = false
            widgetPickerVisible -> {
                widgetPickerVisible = false
                pendingWidgetPlacement = null
            }
            controlsVisible -> controlsVisible = false
            drawerVisible -> drawerVisible = false
            widgetSizeTarget != null -> widgetSizeTarget = null
            renameTarget != null -> renameTarget = null
            contextMenuTarget != null -> contextMenuTarget = null
            folderAppMenuTarget != null -> folderAppMenuTarget = null
            folderItem != null -> {
                folderItem = null
                folderOpenAnchor = null
                navigateToHomePage()
            }
            else -> {
                if (editMode) {
                    editMode = false
                    selectedEmptyTarget = null
                    selectedItemIds = emptySet()
                }
                navigateToHomePage()
            }
        }
    }

    fun launchAndTrack(app: AppCatalogEntry) {
        runCatching { launchApp(context, app) }
            .onFailure { scope.launch { snackbarHostState.showSnackbar(it.message ?: "Unable to launch app") } }
            .onSuccess { layout = repository.recordLaunch(layout, app) }
    }

    fun addPendingShortcut(pending: PendingShortcut) {
        val updated = if (pending.persisted) {
            repository.loadLayout(installedApps)
        } else {
            repository.addShortcut(pending, pagerState.currentPage)
        }
        if (updated == null) {
            scope.launch { snackbarHostState.showSnackbar("No room for new shortcut") }
            return
        }
        layout = updated
        scope.launch { snackbarHostState.showSnackbar("Added \"${pending.label}\"") }
    }

    DisposableEffect(Unit) {
        ShortcutInbox.setListener { shortcut -> addPendingShortcut(shortcut) }
        AppInstallInbox.setListener { refreshSeed += 1 }
        onDispose {
            ShortcutInbox.setListener(null)
            AppInstallInbox.setListener(null)
        }
    }

    fun openHomeSettings() {
        when {
            roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true -> {
                scope.launch { snackbarHostState.showSnackbar("Launcher Layout Studio is already the default home app.") }
            }
            roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true -> {
                homeRoleRequestFallbackPending = true
                homeRoleRequestLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            }
            else -> launchHomeSettingsScreen()
        }
    }

    val autoAction = activity.intent?.getStringExtra("codex_action")
    LaunchedEffect(autoAction) {
        if (!autoAction.isNullOrBlank() && autoAction != handledAutoAction) {
            handledAutoAction = autoAction
            when (autoAction) {
                "request_home_role" -> openHomeSettings()
            }
        }
    }

    fun launchWidgetPicker() {
        val placement = selectedEmptyTarget ?: findFirstAvailableAnywhere(layout, pagerState.currentPage) ?: run {
            scope.launch { snackbarHostState.showSnackbar("No free space for a widget") }
            return
        }
        pendingWidgetPlacement = placement
        widgetPickerVisible = true
    }

    fun launchSystemWidgetPicker() {
        widgetPickerVisible = false
        if (pendingWidgetPlacement == null) return
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            activity.appWidgetHost.deleteAppWidgetId(pendingWidgetId)
        }
        val appWidgetId = activity.appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = appWidgetId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        widgetPickerLauncher.launch(pickIntent)
    }

    fun onCustomWidgetPicked(provider: AppWidgetProviderInfo) {
        val widgetId = activity.appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = widgetId
        val bound = appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider.provider, null)
        if (!bound) {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
            }
            if (bindIntent.resolveActivity(context.packageManager) != null) {
                widgetPickerVisible = false
                widgetPickerLauncher.launch(bindIntent)
                return
            }
            activity.appWidgetHost.deleteAppWidgetId(widgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            scope.launch { snackbarHostState.showSnackbar("Unable to bind this widget") }
            return
        }
        widgetPickerVisible = false
        runWidgetPostBindAfterPick(
            widgetId = widgetId,
            layout = layout,
            pendingPlacement = pendingWidgetPlacement,
            appWidgetManager = appWidgetManager,
            repository = repository,
            activity = activity,
            scope = scope,
            snackbarHostState = snackbarHostState,
            onLayoutCommitted = { layout = it },
            launchConfigure = { widgetConfigureLauncher.launch(it) },
            setWidgetConfigureId = { widgetConfigureId = it },
            clearPendingIfCommitted = { pendingWidgetPlacement = null }
        )
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    fun addAppToCurrentPage(app: AppCatalogEntry) {
        val target = selectedEmptyTarget ?: findFirstAvailableAnywhere(layout, pagerState.currentPage) ?: run {
            scope.launch { snackbarHostState.showSnackbar("This page is full") }
            return
        }
        withUpdatedLayout { current ->
            current.pages[target.pageIndex].items.add(
                HomeItem(
                    type = HomeItemTypes.APP,
                    x = target.x,
                    y = target.y,
                    packageName = app.packageName,
                    activityName = app.activityName
                )
            )
            current
        }
        scope.launch { snackbarHostState.showSnackbar("${app.label} added to the page") }
    }

    fun addAppToDock(app: AppCatalogEntry) {
        val targetSlot = layout.dock.firstEmptyDockSlot()
        if (targetSlot == null) {
            scope.launch { snackbarHostState.showSnackbar("Dock is full") }
            return
        }
        withUpdatedLayout { current ->
            current.dock.add(
                HomeItem(
                    type = HomeItemTypes.APP,
                    x = targetSlot,
                    y = 0,
                    packageName = app.packageName,
                    activityName = app.activityName
                )
            )
            current
        }
    }

    fun addPage() {
        val targetIndex = layout.pages.size
        withUpdatedLayout { current ->
            current.pages.add(PageLayout(index = current.pages.size))
            current.pages.forEachIndexed { index, page -> page.index = index }
            current
        }
        scope.launch {
            pagerState.animateScrollToPage(targetIndex)
            snackbarHostState.showSnackbar("Page ${targetIndex + 1} added")
        }
    }

    fun launchHomeItem(item: HomeItem) {
        val pkg = item.packageName.orEmpty()
        val act = item.activityName.orEmpty()
        if (pkg.isEmpty() || act.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("App link is invalid") }
            return
        }
        val entry = appIndex["$pkg/$act"] ?: AppCatalogEntry(
            label = item.title?.takeIf { it.isNotBlank() } ?: pkg,
            packageName = pkg,
            activityName = act
        )
        launchAndTrack(entry)
    }

    fun movePageBy(delta: Int) {
        val from = pagerState.currentPage
        val to = from + delta
        if (to < 0 || to >= layout.pages.size || delta == 0) return
        withUpdatedLayout { current ->
            val moved = current.pages.removeAt(from)
            current.pages.add(to, moved)
            current.pages.forEachIndexed { idx, page -> page.index = idx }
            current.homePageIndex = when {
                current.homePageIndex == from -> to
                delta > 0 && current.homePageIndex in (from + 1)..to -> current.homePageIndex - 1
                delta < 0 && current.homePageIndex in to until from -> current.homePageIndex + 1
                else -> current.homePageIndex
            }
            current
        }
        scope.launch {
            pagerState.animateScrollToPage(to)
            snackbarHostState.showSnackbar("Page moved to position ${to + 1}")
        }
    }

    fun removeCurrentPage() {
        val currentPage = pagerState.currentPage
        val result = removePage(layout, currentPage)
        if (result == null) {
            scope.launch { snackbarHostState.showSnackbar("Page still has items that do not fit elsewhere") }
        } else {
            commitLayout(result)
            val targetPage = currentPage.coerceAtMost(result.pages.lastIndex)
            scope.launch { pagerState.animateScrollToPage(targetPage) }
        }
    }

    fun setDeleteDropActive(active: Boolean) {
        deleteDropVisible = active
        if (!active) {
            deleteDropHovered = false
        }
    }

    fun updateDeleteDropHover(screenPos: Offset?) {
        deleteDropHovered = screenPos != null && deleteDropBounds?.contains(screenPos) == true
    }

    fun removeWidgetHostIds(items: List<HomeItem>) {
        items.mapNotNull { it.appWidgetId }
            .distinct()
            .forEach(activity.appWidgetHost::deleteAppWidgetId)
    }

    fun removeItemFromHome(target: ItemTarget, message: String = "Item removed") {
        var removed: HomeItem? = null
        withUpdatedLayout { current ->
            removed = current.removeItem(target)
            current
        }
        removed?.let {
            removeWidgetHostIds(listOf(it))
            if (target.area == Area.PAGE) {
                selectedItemIds = selectedItemIds - it.id
            }
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    fun removePageItems(pageIndex: Int, itemIds: Set<String>) {
        if (itemIds.isEmpty()) return
        var removed = emptyList<HomeItem>()
        withUpdatedLayout { current ->
            removed = current.removePageItems(pageIndex, itemIds)
            current
        }
        if (removed.isNotEmpty()) {
            removeWidgetHostIds(removed)
            selectedItemIds = selectedItemIds - removed.map { it.id }.toSet()
            val message = if (removed.size == 1) "Item removed" else "${removed.size} items removed"
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    fun removeAppFromOpenFolder(folderId: String, ref: AppRef) {
        var removed = false
        var updatedFolder: HomeItem? = null
        withUpdatedLayout { current ->
            removed = current.removeAppFromFolder(folderId, ref)
            updatedFolder = current.findFolderById(folderId)?.deepCopy()
            current
        }
        if (removed) {
            folderItem = updatedFolder?.takeIf { it.type == HomeItemTypes.FOLDER && it.appRefs.isNotEmpty() }
            scope.launch { snackbarHostState.showSnackbar("Removed from folder") }
        }
    }

    DisposableEffect(Unit) {
        activity.homeGestureListener = null
        onDispose {
            activity.homeGestureListener = null
            activity.homeGestureIgnoreRegion = null
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootOriginInWindow = it.positionInWindow() }
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            wallpaperBitmap?.let { bitmap ->
                val parallaxPx = with(density) {
                    val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    pageOffset * 8.dp.toPx()
                }
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.08f
                            scaleY = 1.08f
                            translationX = -parallaxPx
                        },
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!editMode) {
                    MinimalHomeHeader(
                        onOpenControls = { controlsVisible = true },
                        onSearch = { drawerVisible = true }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !editMode && pageDragGhost == null && !recentAppsVisible,
                        beyondBoundsPageCount = 1
                    ) { pageIndex ->
                        HomePageCanvas(
                            page = layout.pages.getOrElse(pageIndex) { PageLayout(index = pageIndex) },
                            pageIndex = pageIndex,
                            grid = layout.grid,
                            appIndex = appIndex,
                            iconRepository = iconRepository,
                            activity = activity,
                            appWidgetHost = activity.appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            editMode = editMode,
                            selectedEmptyTarget = selectedEmptyTarget,
                            onSelectEmpty = { selectedEmptyTarget = it },
                            onRequestEditMode = ::enterEditMode,
                            onActivateItem = { item, openAnchor ->
                                when (item.type) {
                                    HomeItemTypes.FOLDER -> {
                                        folderItem = layout.findFolderById(item.id) ?: item
                                        folderOpenAnchor = openAnchor
                                    }
                                    HomeItemTypes.APP -> launchHomeItem(item)
                                    HomeItemTypes.SHORTCUT -> launchShortcut(context, item)
                                }
                            },
                            onLongPressItem = { target ->
                                contextMenuTarget = target
                            },
                            onMoveItem = onMoveItemHandler@{ itemId, x, y, pageDelta, sendToDock, didInsert, dropScreenPos ->
                                val visibleGrid = homeGridInfos[pagerState.currentPage]
                                val visibleCell = visibleGrid?.screenToCell(dropScreenPos.x, dropScreenPos.y)
                                val destinationPageDelta = pagerState.currentPage - pageIndex
                                val resolvedX = visibleCell?.first ?: x
                                val resolvedY = visibleCell?.second ?: y
                                // If dropping into dock area, see if over a dock folder → merge
                                if (sendToDock) {
                                    val slotIdx = dockLayoutInfo?.slotAt(dropScreenPos.x)
                                    val dockFolder = slotIdx?.let { layout.dock.dockItemAt(it) }?.takeIf { it.type == HomeItemTypes.FOLDER }
                                    if (dockFolder != null) {
                                        mergePageItemIntoDockFolder(layout, pageIndex, itemId, dockFolder.id)?.let(::commitLayout)
                                        return@onMoveItemHandler
                                    }
                                }
                                movePageItem(
                                    layout = layout,
                                    pageIndex = pageIndex,
                                    itemId = itemId,
                                    x = resolvedX,
                                    y = resolvedY,
                                    pageDelta = if (sendToDock) 0 else destinationPageDelta.takeIf { it != 0 } ?: pageDelta,
                                    sendToDock = sendToDock,
                                    didInsert = didInsert,
                                    dockTargetSlot = if (sendToDock) dockLayoutInfo?.slotAt(dropScreenPos.x) else null
                                )?.let(::commitLayout)
                            },
                            onEdgePageRequest = { direction ->
                                val targetPage = pagerState.currentPage + direction
                                if (targetPage !in layout.pages.indices) {
                                    false
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(targetPage) }
                                    true
                                }
                            },
                            onGridLayoutChange = { info ->
                                homeGridInfos[info.pageIndex] = info
                                if (info.pageIndex == pagerState.currentPage) {
                                    homeGridInfo = info
                                }
                            },
                            onDragGhostChange = { pageDragGhost = it },
                            selectedItemIds = selectedItemIds,
                            onToggleSelect = { id ->
                                selectedItemIds = if (selectedItemIds.contains(id)) {
                                    selectedItemIds - id
                                } else {
                                    selectedItemIds + id
                                }
                            },
                            onBulkMove = { ids, leaderId, targetX, targetY, pageDelta, sendToDock, hoveredItemId ->
                                val updated = bulkMoveItems(layout, pageIndex, ids, leaderId, targetX, targetY, pageDelta, sendToDock, hoveredItemId)
                                if (updated != null) {
                                    commitLayout(updated)
                                    selectedItemIds = emptySet()
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Couldn't move all selected apps") }
                                }
                            },
                            onPeekItem = { target -> peekTarget = target },
                            onDismissPeek = { peekTarget = null },
                            deleteDropBounds = deleteDropBounds,
                            onDragStateChange = ::setDeleteDropActive,
                            onDeleteHoverPositionChange = ::updateDeleteDropHover,
                            onDeleteItems = ::removePageItems,
                            onResizeWidget = { itemId, newSpanX, newSpanY ->
                                val target = ItemTarget(Area.PAGE, pageIndex, itemId)
                                withUpdatedLayout { current ->
                                    current.adjustWidgetSize(target, newSpanX, newSpanY)
                                    current
                                }
                            },
                            isLocked = layout.isLocked
                        )
                    }
                }

                if (editMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { launchWidgetPicker() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Add widget", color = Color.White) }
                        OutlinedButton(
                            onClick = { drawerVisible = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("App drawer", color = Color.White) }
                    }
                }

                PageIndicator(count = layout.pages.size, currentPage = pagerState.currentPage, homePage = homePageIndex)
                if (!editMode && dockRecentApps.isNotEmpty()) {
                    RecentAppsDockRow(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .align(Alignment.CenterHorizontally),
                        recentApps = dockRecentApps,
                        iconRepository = iconRepository,
                        onLaunch = ::launchAndTrack,
                        onPressedChange = { recentAppsStripPressed = it },
                        onBoundsChange = { activity.homeGestureIgnoreRegion = it }
                    )
                }
                DockBar(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .align(Alignment.CenterHorizontally),
                    dockItems = layout.dock,
                    appIndex = appIndex,
                    iconRepository = iconRepository,
                    editMode = editMode,
                    onRequestEditMode = ::enterEditMode,
                    onMoveItem = { itemId, targetIndex ->
                        reorderDock(layout, itemId, targetIndex)?.let(::commitLayout)
                    },
                    onMoveToPage = { itemId, screenPos ->
                        val info = homeGridInfo
                        val cell = info?.screenToCell(screenPos.x, screenPos.y)
                        val targetPage = info?.pageIndex ?: pagerState.currentPage
                        val updated = moveDockItemToPage(layout, itemId, targetPage, cell?.first, cell?.second)
                        if (updated != null) {
                            commitLayout(updated)
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("No space on that page") }
                        }
                    },
                    onActivate = { item, openAnchor ->
                        when (item.type) {
                            HomeItemTypes.FOLDER -> {
                                folderItem = layout.findFolderById(item.id) ?: item
                                folderOpenAnchor = openAnchor
                            }
                            HomeItemTypes.APP -> launchHomeItem(item)
                            HomeItemTypes.SHORTCUT -> launchShortcut(context, item)
                        }
                    },
                    onLongPress = { item ->
                        contextMenuTarget = ItemTarget(Area.DOCK, itemId = item.id)
                    },
                    onPeek = { item -> peekTarget = ItemTarget(Area.DOCK, itemId = item.id) },
                    onDismissPeek = { peekTarget = null },
                    onDockLayoutChange = { dockLayoutInfo = it },
                    deleteDropBounds = deleteDropBounds,
                    onDragStateChange = ::setDeleteDropActive,
                    onDeleteHoverPositionChange = ::updateDeleteDropHover,
                    onDeleteDockItem = { itemId -> removeItemFromHome(ItemTarget(Area.DOCK, itemId = itemId)) },
                    isLocked = layout.isLocked
                )
            }

            pageDragGhost?.let { ghost ->
                val localCenter = ghost.centerInWindow - rootOriginInWindow
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (localCenter.x - ghost.widthPx / 2f).roundToInt(),
                                (localCenter.y - ghost.heightPx / 2f).roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { ghost.widthPx.toDp() },
                            height = with(density) { ghost.heightPx.toDp() }
                        )
                        .graphicsLayer {
                            alpha = 0.94f
                            scaleX = 1.10f
                            scaleY = 1.10f
                        }
                ) {
                    when (ghost.item.type) {
                        HomeItemTypes.FOLDER -> FolderTile(ghost.item, appIndex, iconRepository, large = false)
                        HomeItemTypes.SHORTCUT -> ShortcutTile(ghost.item, large = false)
                        else -> AppTile(ghost.item, appIndex, iconRepository, large = false)
                    }
                }
            }

            AnimatedVisibility(
                visible = recentAppsVisible,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                RecentAppsOverlay(
                    recentApps = recentApps,
                    iconRepository = iconRepository,
                    onLaunch = {
                        recentAppsVisible = false
                        launchAndTrack(it)
                    },
                    onDismiss = { recentAppsVisible = false }
                )
            }

            AnimatedVisibility(visible = folderItem != null, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                folderItem?.let { item ->
                    FolderOverlay(
                        item = item,
                        appIndex = appIndex,
                        iconRepository = iconRepository,
                        editMode = editMode,
                        wallpaperBitmap = wallpaperBitmap,
                        openAnchor = folderOpenAnchor,
                        onReorder = { reordered ->
                            withUpdatedLayout { current ->
                                current.updateFolderApps(item.id, reordered)
                                current
                            }
                            folderItem = folderItem?.copy(appRefs = reordered)
                        },
                        onDismiss = {
                            folderItem = null
                            folderOpenAnchor = null
                        },
                        onLaunch = {
                            if (editMode) return@FolderOverlay
                            folderItem = null
                            folderOpenAnchor = null
                            launchAndTrack(it)
                        },
                        onExtractAt = { ref, screenPos ->
                            val gridInfo = homeGridInfo
                            val targetCell = gridInfo?.screenToCell(screenPos.x, screenPos.y)
                            var extracted = false
                            var updatedFolder: HomeItem? = null
                            val targetPage = gridInfo?.pageIndex ?: pagerState.currentPage
                            withUpdatedLayout { current ->
                                extracted = current.extractAppFromFolderToTarget(item.id, ref, targetPage, targetCell?.first, targetCell?.second)
                                updatedFolder = current.findFolderById(item.id)?.deepCopy()
                                current
                            }
                            if (extracted) {
                                folderItem = updatedFolder?.takeIf { it.appRefs.isNotEmpty() }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("No free space to pull that app onto the page") }
                            }
                        },
                        onRename = {
                            val location = locateItem(layout, item.id)
                            if (location != null) {
                                renameDraft = item.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Folder"
                                renameTarget = location
                                folderItem = null
                                folderOpenAnchor = null
                            }
                        },
                        onRemove = {
                            val location = locateItem(layout, item.id)
                            if (location != null) {
                                removeItemFromHome(location)
                                folderItem = null
                                folderOpenAnchor = null
                            }
                        },
                        deleteDropBounds = deleteDropBounds,
                        onDragStateChange = ::setDeleteDropActive,
                        onDeleteHoverPositionChange = ::updateDeleteDropHover,
                        onDeleteFolderApp = { ref -> removeAppFromOpenFolder(item.id, ref) },
                        onShowAppMenu = { ref, app ->
                            folderAppMenuTarget = FolderAppMenuTarget(item.id, ref, app)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = deleteDropVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                DeleteDropTarget(
                    hovered = deleteDropHovered,
                    onBoundsChange = { deleteDropBounds = it }
                )
            }

            AnimatedVisibility(
                visible = isBootstrapLoading,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 88.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Syncing apps in background...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (drawerVisible) {
        ModalBottomSheet(onDismissRequest = { drawerVisible = false }) {
            AppDrawerSheet(
                apps = installedApps,
                recentApps = recentApps,
                iconRepository = iconRepository,
                onLaunch = {
                    drawerVisible = false
                    launchAndTrack(it)
                },
                onAddToHome = ::addAppToCurrentPage,
                onAddToDock = ::addAppToDock
            )
        }
    }

    if (controlsVisible) {
        ModalBottomSheet(onDismissRequest = { controlsVisible = false }) {
            HomeControlsSheet(
                currentPage = pagerState.currentPage,
                pageCount = layout.pages.size,
                isCurrentHome = pagerState.currentPage == homePageIndex,
                hasWallpaper = !layout.wallpaperPath.isNullOrBlank(),
                isLocked = layout.isLocked,
                onToggleLock = {
                    val nextLocked = !layout.isLocked
                    withUpdatedLayout { current ->
                        current.isLocked = nextLocked
                        current
                    }
                    scope.launch { snackbarHostState.showSnackbar(if (nextLocked) "Layout locked" else "Layout unlocked") }
                },
                onEdit = {
                    enterEditMode()
                },
                onAddWidget = {
                    controlsVisible = false
                    launchWidgetPicker()
                },
                onAddPage = {
                    controlsVisible = false
                    addPage()
                },
                onRemovePage = {
                    controlsVisible = false
                    removeCurrentPage()
                },
                onOpenDrawer = {
                    controlsVisible = false
                    drawerVisible = true
                },
                onChooseHome = {
                    controlsVisible = false
                    openHomeSettings()
                },
                onSetCurrentAsHome = {
                    controlsVisible = false
                    val target = pagerState.currentPage
                    withUpdatedLayout { current ->
                        current.homePageIndex = target
                        current
                    }
                    scope.launch { snackbarHostState.showSnackbar("Page ${target + 1} set as home") }
                },
                onPickWallpaper = {
                    controlsVisible = false
                    wallpaperPickerLauncher.launch(arrayOf("image/*"))
                },
                onClearWallpaper = {
                    controlsVisible = false
                    repository.clearWallpaper()
                    withUpdatedLayout { current ->
                        current.wallpaperPath = null
                        current
                    }
                    scope.launch { snackbarHostState.showSnackbar("Wallpaper cleared") }
                },
                onExportApps = {
                    controlsVisible = false
                    exportAppsLauncher.launch("apps.json")
                },
                onExportBackup = {
                    controlsVisible = false
                    exportBackupLauncher.launch("launcher-backup.json")
                },
                onImportBackup = {
                    controlsVisible = false
                    importBackupLauncher.launch(arrayOf("application/json"))
                },
                canApplyOriginalLayout = canApplyOriginalLayout,
                onApplyOriginalLayout = {
                    controlsVisible = false
                    runCatching { repository.importOriginalLayout() }
                        .onSuccess { imported ->
                            layout = imported
                            selectedEmptyTarget = null
                            selectedItemIds = emptySet()
                            folderItem = null
                            folderOpenAnchor = null
                            scope.launch { snackbarHostState.showSnackbar("Original layout applied") }
                        }
                        .onFailure { scope.launch { snackbarHostState.showSnackbar(it.message ?: "Original layout import failed") } }
                },
                onReload = {
                    controlsVisible = false
                    refreshSeed += 1
                },
                floatingButtonEnabled = floatingButtonEnabled,
                onToggleFloatingButton = {
                    controlsVisible = false
                    toggleFloatingButton()
                },
                floatingAlpha = floatingAlpha,
                floatingOuterDp = floatingOuterDp,
                floatingIconDp = floatingIconDp,
                floatingFlingTrigger = floatingFlingTrigger,
                onSetFloatingAlpha = {
                    floatingAlpha = it
                    FloatingButtonService.setAlpha(context, it)
                },
                onSetFloatingOuterDp = {
                    floatingOuterDp = it
                    FloatingButtonService.setOuterSize(context, it)
                    if (floatingIconDp > it) {
                        floatingIconDp = it
                        FloatingButtonService.setIconSize(context, it)
                    }
                },
                onSetFloatingIconDp = {
                    floatingIconDp = it
                    FloatingButtonService.setIconSize(context, it)
                },
                onSetFloatingFlingTrigger = {
                    floatingFlingTrigger = it
                    FloatingButtonService.setFlingTriggerVelocity(context, it)
                },
                onMovePageLeft = {
                    controlsVisible = false
                    movePageBy(-1)
                },
                onMovePageRight = {
                    controlsVisible = false
                    movePageBy(1)
                },
                groupedMenu = isParallelVariant,
                onOpenRecentApps = {
                    controlsVisible = false
                    recentAppsVisible = true
                }
            )
        }
    }

    if (widgetPickerVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                widgetPickerVisible = false
                pendingWidgetPlacement = null
            }
        ) {
            WidgetPickerSheet(
                grid = layout.grid,
                providers = widgetProviders,
                loading = widgetListLoading,
                onDismiss = {
                    widgetPickerVisible = false
                    pendingWidgetPlacement = null
                },
                onPick = ::onCustomWidgetPicked,
                onUseSystemPicker = ::launchSystemWidgetPicker
            )
        }
    }

    contextMenuTarget?.let { target ->
        val item = layout.findItem(target)
        if (item != null) {
            ItemActionDialog(
                item = item,
                appIndex = appIndex,
                editMode = editMode,
                canPinToDock = target.area == Area.PAGE && item.type != HomeItemTypes.WIDGET && layout.dock.firstEmptyDockSlot() != null,
                canSendBackToPage = target.area == Area.DOCK,
                canResize = item.type == HomeItemTypes.WIDGET && isWidgetResizable(appWidgetManager, item),
                canUninstallOrInfo = item.type == HomeItemTypes.APP && !item.packageName.isNullOrBlank(),
                onAppInfo = {
                    contextMenuTarget = null
                    item.packageName?.let { pkg ->
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            scope.launch { snackbarHostState.showSnackbar(it.message ?: "Cannot open app info") }
                        }
                    }
                },
                onUninstall = {
                    contextMenuTarget = null
                    item.packageName?.let { pkg ->
                        runCatching {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = android.net.Uri.fromParts("package", pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            scope.launch { snackbarHostState.showSnackbar(it.message ?: "Cannot uninstall") }
                        }
                    }
                },
                onDismiss = { contextMenuTarget = null },
                onOpen = {
                    contextMenuTarget = null
                    when (item.type) {
                        HomeItemTypes.FOLDER -> folderItem = item
                        HomeItemTypes.APP -> launchHomeItem(item)
                        HomeItemTypes.SHORTCUT -> launchShortcut(context, item)
                    }
                },
                onRename = {
                    renameDraft = item.title ?: item.labelOrFallback(appIndex)
                    renameTarget = target
                    contextMenuTarget = null
                },
                onRemove = {
                    removeItemFromHome(target)
                    contextMenuTarget = null
                },
                onPinToDock = {
                    withUpdatedLayout { current ->
                        current.moveItemToDock(target)
                        current
                    }
                    contextMenuTarget = null
                },
                onSendBackToPage = {
                    withUpdatedLayout { current ->
                        current.moveItemFromDockToPage(target, pagerState.currentPage)
                        current
                    }
                    contextMenuTarget = null
                },
                onResize = {
                    widgetSizeTarget = WidgetSizeTarget(target, item.spanX, item.spanY)
                    contextMenuTarget = null
                }
            )
        } else {
            contextMenuTarget = null
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            value = renameDraft,
            onValueChange = { renameDraft = it },
            onDismiss = { renameTarget = null },
            onSave = {
                withUpdatedLayout { current ->
                    current.renameItem(target, renameDraft)
                    current
                }
                renameTarget = null
            }
        )
    }

    widgetSizeTarget?.let { target ->
        WidgetSizeDialog(
            spanX = target.spanX,
            spanY = target.spanY,
            maxSpanX = layout.grid.cols,
            maxSpanY = layout.grid.rows,
            onDismiss = { widgetSizeTarget = null },
            onSave = { spanX, spanY ->
                withUpdatedLayout { current ->
                    current.adjustWidgetSize(target.itemTarget, spanX, spanY)
                    current
                }
                widgetSizeTarget = null
            }
        )
    }

    peekTarget?.let { target ->
        val item = layout.findItem(target)
        if (item != null) {
            PeekActionMenu(
                item = item,
                editMode = editMode,
                onDismiss = { peekTarget = null },
                onAppInfo = {
                    peekTarget = null
                    item.packageName?.let { pkg ->
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            scope.launch { snackbarHostState.showSnackbar(it.message ?: "Cannot open app info") }
                        }
                    }
                },
                onUninstall = {
                    peekTarget = null
                    item.packageName?.let { pkg ->
                        runCatching {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = android.net.Uri.fromParts("package", pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            scope.launch { snackbarHostState.showSnackbar(it.message ?: "Cannot uninstall") }
                        }
                    }
                },
                onRename = {
                    renameDraft = item.title?.trim()?.takeIf { it.isNotEmpty() } ?: item.labelOrFallback(appIndex)
                    renameTarget = target
                    peekTarget = null
                },
                onRemoveFromHome = {
                    removeItemFromHome(target)
                    peekTarget = null
                }
            )
        } else {
            peekTarget = null
        }
    }

    folderAppMenuTarget?.let { target ->
        FolderAppActionDialog(
            app = target.app,
            onDismiss = { folderAppMenuTarget = null },
            onOpen = {
                folderAppMenuTarget = null
                launchAndTrack(target.app)
            },
            onAppInfo = {
                folderAppMenuTarget = null
                runCatching {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", target.app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar(it.message ?: "Cannot open app info") }
                }
            },
            onUninstall = {
                folderAppMenuTarget = null
                runCatching {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.fromParts("package", target.app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar(it.message ?: "Cannot uninstall") }
                }
            },
            onRemoveFromFolder = {
                folderAppMenuTarget = null
                removeAppFromOpenFolder(target.folderId, target.ref)
            }
        )
    }
}

@Composable
private fun PeekActionMenu(
    item: HomeItem,
    editMode: Boolean,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onRename: () -> Unit,
    onRemoveFromHome: () -> Unit
) {
    val canUninstallOrInfo = item.type == HomeItemTypes.APP && !item.packageName.isNullOrBlank()
    Popup(
        alignment = Alignment.TopCenter,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false),
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .padding(top = 56.dp, start = 24.dp, end = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.title?.takeIf { it.isNotBlank() } ?: (item.packageName ?: "Item"),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Text("×", style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (canUninstallOrInfo) {
                    OutlinedButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth()) { Text("App info") }
                    OutlinedButton(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) { Text("Uninstall") }
                }
                if (editMode) {
                    OutlinedButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) { Text("Rename") }
                    OutlinedButton(onClick = onRemoveFromHome, modifier = Modifier.fillMaxWidth()) { Text("Remove from home") }
                }
            }
        }
    }
}

@Composable
private fun FolderAppActionDialog(
    app: AppCatalogEntry,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onRemoveFromFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Text("Open")
                }
                OutlinedButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth()) {
                    Text("App info")
                }
                OutlinedButton(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) {
                    Text("Uninstall")
                }
                OutlinedButton(onClick = onRemoveFromFolder, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove from folder")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun MinimalHomeHeader(
    onOpenControls: () -> Unit,
    onSearch: () -> Unit
) {
    val now by produceState(initialValue = java.time.LocalDateTime.now()) {
        while (true) {
            value = java.time.LocalDateTime.now()
            delay(1_000)
        }
    }

    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val rocYear = now.year - 1911
    val dateText = "(${rocYear}年)%04d.%02d.%02d %s".format(
        now.year,
        now.monthValue,
        now.dayOfMonth,
        dayNames[now.dayOfWeek.value - 1]
    )
    val timeText = String.format("%02d:%02d", now.hour, now.minute)
    val secondsText = String.format("%02d", now.second)
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xCC111827),
                            Color(0xB31F2937),
                            Color(0x992B3548)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = dateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFE5E7EB)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = secondsText,
                        modifier = Modifier.padding(bottom = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF93C5FD)
                    )
                }
            }
            FilledIconButton(
                onClick = onSearch,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search")
            }
            FilledIconButton(
                onClick = onOpenControls,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Controls")
            }
        }
    }
}

@Composable
private fun SearchPill(modifier: Modifier = Modifier, compact: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onClick)
                .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White)
                Text(
                    "Search apps",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            if (!compact) {
                Icon(Icons.Rounded.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun WidgetPickerSheet(
    grid: GridSpec,
    providers: List<AppWidgetProviderInfo>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onUseSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val labelMap = remember(providers) {
        providers.associateWith { it.loadLabel(context.packageManager).toString() }
    }
    val filteredProviders = remember(labelMap, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            providers
        } else {
            providers.filter { info ->
                val label = labelMap[info].orEmpty()
                label.contains(q, ignoreCase = true) ||
                    info.provider.packageName.contains(q, ignoreCase = true)
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Widgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onDismiss) { Text("Close") }
        }
        Text(
            "「約 N×M 格」依主畫面格線與小工具 min 尺寸估算；若該格無空位會自動找其他位置。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Search widgets") },
            singleLine = true
        )
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            providers.isEmpty() -> {
                Text("No widgets found", style = MaterialTheme.typography.bodyMedium)
            }
            filteredProviders.isEmpty() -> {
                Text(
                    "No widgets match \"${searchQuery.trim()}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(filteredProviders, key = { it.provider.flattenToString() }) { info ->
                        val label = labelMap[info] ?: info.loadLabel(context.packageManager).toString()
                        val (spanX, spanY) = estimateWidgetSpan(info, grid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onPick(info) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    label,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    widgetPlacementSummaryLabel(info, grid),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${spanX} x ${spanY}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onUseSystemPicker, modifier = Modifier.fillMaxWidth()) {
            Text("Use system widget list")
        }
    }
}

@Composable
private fun HomeControlsSheet(
    currentPage: Int,
    pageCount: Int,
    isCurrentHome: Boolean,
    hasWallpaper: Boolean,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onEdit: () -> Unit,
    onOpenDrawer: () -> Unit,
    onAddWidget: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onChooseHome: () -> Unit,
    onSetCurrentAsHome: () -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onExportApps: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    canApplyOriginalLayout: Boolean,
    onApplyOriginalLayout: () -> Unit,
    onReload: () -> Unit,
    floatingButtonEnabled: Boolean,
    onToggleFloatingButton: () -> Unit,
    floatingAlpha: Float,
    floatingOuterDp: Int,
    floatingIconDp: Int,
    floatingFlingTrigger: Int,
    onSetFloatingAlpha: (Float) -> Unit,
    onSetFloatingOuterDp: (Int) -> Unit,
    onSetFloatingIconDp: (Int) -> Unit,
    onSetFloatingFlingTrigger: (Int) -> Unit,
    onMovePageLeft: () -> Unit,
    onMovePageRight: () -> Unit,
    groupedMenu: Boolean,
    onOpenRecentApps: () -> Unit
) {
    if (groupedMenu) {
        GroupedHomeControlsSheet(
            currentPage = currentPage,
            pageCount = pageCount,
            isCurrentHome = isCurrentHome,
            hasWallpaper = hasWallpaper,
            isLocked = isLocked,
            onToggleLock = onToggleLock,
            onEdit = onEdit,
            onOpenDrawer = onOpenDrawer,
            onAddWidget = onAddWidget,
            onAddPage = onAddPage,
            onRemovePage = onRemovePage,
            onChooseHome = onChooseHome,
            onSetCurrentAsHome = onSetCurrentAsHome,
            onPickWallpaper = onPickWallpaper,
            onClearWallpaper = onClearWallpaper,
            onExportApps = onExportApps,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            canApplyOriginalLayout = canApplyOriginalLayout,
            onApplyOriginalLayout = onApplyOriginalLayout,
            onReload = onReload,
            floatingButtonEnabled = floatingButtonEnabled,
            onToggleFloatingButton = onToggleFloatingButton,
            floatingAlpha = floatingAlpha,
            floatingOuterDp = floatingOuterDp,
            floatingIconDp = floatingIconDp,
            floatingFlingTrigger = floatingFlingTrigger,
            onSetFloatingAlpha = onSetFloatingAlpha,
            onSetFloatingOuterDp = onSetFloatingOuterDp,
            onSetFloatingIconDp = onSetFloatingIconDp,
            onSetFloatingFlingTrigger = onSetFloatingFlingTrigger,
            onMovePageLeft = onMovePageLeft,
            onMovePageRight = onMovePageRight,
            onOpenRecentApps = onOpenRecentApps
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        Text("Home Controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Page ${currentPage + 1} of $pageCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onToggleFloatingButton, modifier = Modifier.fillMaxWidth()) {
            Text(if (floatingButtonEnabled) "Disable floating button" else "Enable floating button")
        }
        if (floatingButtonEnabled) {
            FloatingButtonSettings(
                alpha = floatingAlpha,
                outerDp = floatingOuterDp,
                iconDp = floatingIconDp,
                flingTrigger = floatingFlingTrigger,
                onAlphaChange = onSetFloatingAlpha,
                onOuterChange = onSetFloatingOuterDp,
                onIconChange = onSetFloatingIconDp,
                onFlingTriggerChange = onSetFloatingFlingTrigger
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onAddPage, modifier = Modifier.weight(1f)) { Text("Add page") }
            OutlinedButton(
                onClick = onRemovePage,
                enabled = pageCount > 1,
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete page")
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onMovePageLeft,
                enabled = currentPage > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("← Move page left")
            }
            OutlinedButton(
                onClick = onMovePageRight,
                enabled = currentPage < pageCount - 1,
                modifier = Modifier.weight(1f)
            ) {
                Text("Move page right →")
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit layout") }
            OutlinedButton(onClick = onAddWidget, modifier = Modifier.weight(1f)) { Text("Add widget") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onOpenDrawer, modifier = Modifier.weight(1f)) { Text("Open app drawer") }
            OutlinedButton(onClick = onToggleLock, modifier = Modifier.weight(1f)) {
                Text(if (isLocked) "Unlock layout" else "Lock layout")
            }
        }
        OutlinedButton(
            onClick = onSetCurrentAsHome,
            enabled = !isCurrentHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCurrentHome) "Page ${currentPage + 1} is home" else "Set page ${currentPage + 1} as home")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPickWallpaper, modifier = Modifier.weight(1f)) { Text("Choose wallpaper") }
            if (hasWallpaper) {
                OutlinedButton(onClick = onClearWallpaper, modifier = Modifier.weight(1f)) { Text("Clear wallpaper") }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onExportApps, modifier = Modifier.weight(1f)) { Text("Export apps") }
            OutlinedButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) { Text("Export backup") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) { Text("Restore backup") }
            if (canApplyOriginalLayout) {
                OutlinedButton(onClick = onApplyOriginalLayout, modifier = Modifier.weight(1f)) {
                    Text("Apply original layout")
                }
            } else {
                OutlinedButton(onClick = onReload, modifier = Modifier.weight(1f)) { Text("Reload apps") }
            }
        }
        if (canApplyOriginalLayout) {
            OutlinedButton(onClick = onReload, modifier = Modifier.fillMaxWidth()) { Text("Reload apps") }
        }
            OutlinedButton(onClick = onChooseHome, modifier = Modifier.fillMaxWidth()) { Text("Choose home app") }
        }
    }
}

@Composable
private fun GroupedHomeControlsSheet(
    currentPage: Int,
    pageCount: Int,
    isCurrentHome: Boolean,
    hasWallpaper: Boolean,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onEdit: () -> Unit,
    onOpenDrawer: () -> Unit,
    onAddWidget: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onChooseHome: () -> Unit,
    onSetCurrentAsHome: () -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onExportApps: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    canApplyOriginalLayout: Boolean,
    onApplyOriginalLayout: () -> Unit,
    onReload: () -> Unit,
    floatingButtonEnabled: Boolean,
    onToggleFloatingButton: () -> Unit,
    floatingAlpha: Float,
    floatingOuterDp: Int,
    floatingIconDp: Int,
    floatingFlingTrigger: Int,
    onSetFloatingAlpha: (Float) -> Unit,
    onSetFloatingOuterDp: (Int) -> Unit,
    onSetFloatingIconDp: (Int) -> Unit,
    onSetFloatingFlingTrigger: (Int) -> Unit,
    onMovePageLeft: () -> Unit,
    onMovePageRight: () -> Unit,
    onOpenRecentApps: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Home controls",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Parallel test build · Page ${currentPage + 1} of $pageCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = if (isLocked) "Layout locked" else "Layout unlocked",
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Layout status", style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (isLocked) "Locked · editing is disabled" else "Unlocked · ready to edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onToggleLock) {
                    Text(if (isLocked) "Unlock" else "Lock")
                }
            }
        }

        ControlsGroup(
            title = "Desktop & pages",
            subtitle = "Organize the current home screen",
            icon = Icons.Rounded.Home
        ) {
            ControlRow(
                icon = Icons.Rounded.Edit,
                title = "Edit layout",
                description = "Move, resize, and organize items",
                onClick = onEdit
            )
            ControlRow(
                icon = Icons.Rounded.Add,
                title = "Add page",
                description = "Create a new home page",
                onClick = onAddPage
            )
            ControlRow(
                icon = Icons.Rounded.Delete,
                title = "Delete page",
                description = if (pageCount > 1) "Remove page ${currentPage + 1}" else "At least one page is required",
                onClick = onRemovePage,
                enabled = pageCount > 1
            )
            ControlRow(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                title = "Move page left",
                description = "Reorder page ${currentPage + 1}",
                onClick = onMovePageLeft,
                enabled = currentPage > 0
            )
            ControlRow(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                title = "Move page right",
                description = "Reorder page ${currentPage + 1}",
                onClick = onMovePageRight,
                enabled = currentPage < pageCount - 1,
                iconModifier = Modifier.graphicsLayer(scaleX = -1f)
            )
            ControlRow(
                icon = Icons.Rounded.Home,
                title = if (isCurrentHome) "Home page selected" else "Set current page as home",
                description = "Page ${currentPage + 1} is the first screen shown",
                onClick = onSetCurrentAsHome,
                enabled = !isCurrentHome
            )
            ControlRow(
                icon = Icons.Rounded.Wallpaper,
                title = "Choose wallpaper",
                description = if (hasWallpaper) "Replace the current wallpaper" else "Set a wallpaper for the launcher",
                onClick = onPickWallpaper
            )
            if (hasWallpaper) {
                ControlRow(
                    icon = Icons.Rounded.Delete,
                    title = "Clear wallpaper",
                    description = "Return to the default background",
                    onClick = onClearWallpaper
                )
            }
        }

        ControlsGroup(
            title = "Apps & recent",
            subtitle = "Find apps and keep frequent actions close",
            icon = Icons.Rounded.Apps
        ) {
            ControlRow(
                icon = Icons.Rounded.Apps,
                title = "Open app drawer",
                description = "Browse all installed apps",
                onClick = onOpenDrawer
            )
            ControlRow(
                icon = Icons.Rounded.Widgets,
                title = "Add widget",
                description = "Place a widget on the current page",
                onClick = onAddWidget
            )
            ControlStatusRow(
                icon = Icons.Rounded.Folder,
                title = "Folder previews",
                description = "Show 4 icons outside; open the folder to see all apps",
                value = "4 visible"
            )
            ControlRow(
                icon = Icons.Rounded.History,
                title = "Recently opened apps",
                description = "Browse up to 15 apps in the recent strip",
                onClick = onOpenRecentApps,
                trailing = { ControlValue("15") }
            )
            ControlRow(
                icon = Icons.Rounded.Refresh,
                title = "Reload apps",
                description = "Refresh the installed-app catalog",
                onClick = onReload
            )
        }

        ControlsGroup(
            title = "Backup & test",
            subtitle = "Protect and move launcher configurations",
            icon = Icons.Rounded.Backup
        ) {
            ControlRow(
                icon = Icons.Rounded.Backup,
                title = "Export apps",
                description = "Save the installed-app catalog as JSON",
                onClick = onExportApps
            )
            ControlRow(
                icon = Icons.Rounded.Backup,
                title = "Export backup",
                description = "Save the current layout as JSON",
                onClick = onExportBackup
            )
            ControlRow(
                icon = Icons.Rounded.Restore,
                title = "Restore backup",
                description = "Replace this test layout from a saved file",
                onClick = onImportBackup
            )
            if (canApplyOriginalLayout) {
                ControlRow(
                    icon = Icons.Rounded.Restore,
                    title = "Apply original layout",
                    description = "Read the original app configuration into parallel",
                    onClick = onApplyOriginalLayout
                )
            }
            ControlRow(
                icon = Icons.Rounded.Settings,
                title = "Choose home app",
                description = "Open Android's default home-app settings",
                onClick = onChooseHome
            )
        }

        ControlsGroup(
            title = "Floating button",
            subtitle = "Optional quick access from any screen",
            icon = Icons.Rounded.Tune
        ) {
            ControlRow(
                icon = Icons.Rounded.Tune,
                title = if (floatingButtonEnabled) "Floating button enabled" else "Floating button disabled",
                description = if (floatingButtonEnabled) "Tap to open the launcher controls" else "Enable an overlay shortcut",
                onClick = onToggleFloatingButton,
                trailing = { ControlValue(if (floatingButtonEnabled) "On" else "Off") }
            )
            if (floatingButtonEnabled) {
                FloatingButtonSettings(
                    alpha = floatingAlpha,
                    outerDp = floatingOuterDp,
                    iconDp = floatingIconDp,
                    flingTrigger = floatingFlingTrigger,
                    onAlphaChange = onSetFloatingAlpha,
                    onOuterChange = onSetFloatingOuterDp,
                    onIconChange = onSetFloatingIconDp,
                    onFlingTriggerChange = onSetFloatingFlingTrigger
                )
            }
        }
    }
}

@Composable
private fun ControlsGroup(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ControlRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    iconModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.42f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(icon, contentDescription = null, modifier = iconModifier.padding(7.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke() ?: Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ControlStatusRow(
    icon: ImageVector,
    title: String,
    description: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(7.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        ControlValue(value)
    }
}

@Composable
private fun ControlValue(value: String) {
    Text(
        value,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1
    )
}

@Composable
private fun FloatingButtonSettings(
    alpha: Float,
    outerDp: Int,
    iconDp: Int,
    flingTrigger: Int,
    onAlphaChange: (Float) -> Unit,
    onOuterChange: (Int) -> Unit,
    onIconChange: (Int) -> Unit,
    onFlingTriggerChange: (Int) -> Unit
) {
    DisposableEffect(Unit) {
        FloatingButtonStateBus.setSettingsVisible(true)
        onDispose { FloatingButtonStateBus.setSettingsVisible(false) }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("懸浮球設定", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "螢幕上的真實懸浮球會即時跟著滑桿變化。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = (FloatingButtonService.MAX_OUTER_DP + 16).dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingButtonPreview(alpha = alpha, outerDp = outerDp, iconDp = iconDp)
                }
            }
        }

        FloatingButtonSliderCard(
            title = "透明度",
            valueText = "${(alpha * 100).toInt()}%",
            sliderValue = alpha,
            onSliderChange = onAlphaChange,
            valueRange = FloatingButtonService.MIN_ALPHA..FloatingButtonService.MAX_ALPHA,
            steps = 0
        )
        FloatingButtonSliderCard(
            title = "外圈大小",
            valueText = "$outerDp dp",
            sliderValue = outerDp.toFloat(),
            onSliderChange = { onOuterChange(it.toInt()) },
            valueRange = FloatingButtonService.MIN_OUTER_DP.toFloat()..FloatingButtonService.MAX_OUTER_DP.toFloat(),
            steps = (FloatingButtonService.MAX_OUTER_DP - FloatingButtonService.MIN_OUTER_DP - 1).coerceAtLeast(0)
        )
        FloatingButtonSliderCard(
            title = "中心圖標",
            valueText = "${iconDp.coerceAtMost(outerDp)} dp",
            sliderValue = iconDp.coerceAtMost(outerDp).toFloat(),
            onSliderChange = { onIconChange(it.toInt()) },
            valueRange = FloatingButtonService.MIN_ICON_DP.toFloat()..FloatingButtonService.MAX_ICON_DP.toFloat(),
            steps = (FloatingButtonService.MAX_ICON_DP - FloatingButtonService.MIN_ICON_DP - 1).coerceAtLeast(0)
        )
        FloatingButtonSliderCard(
            title = "甩動觸發速度",
            valueText = "$flingTrigger px/s",
            sliderValue = flingTrigger.toFloat(),
            onSliderChange = { raw ->
                val step = FloatingButtonService.FLING_TRIGGER_STEP_PX_PER_SEC
                val snapped = (raw / step).roundToInt() * step
                onFlingTriggerChange(
                    snapped.coerceIn(
                        FloatingButtonService.MIN_FLING_TRIGGER_PX_PER_SEC,
                        FloatingButtonService.MAX_FLING_TRIGGER_PX_PER_SEC
                    )
                )
            },
            valueRange = FloatingButtonService.MIN_FLING_TRIGGER_PX_PER_SEC.toFloat()..
                FloatingButtonService.MAX_FLING_TRIGGER_PX_PER_SEC.toFloat(),
            steps = (
                (FloatingButtonService.MAX_FLING_TRIGGER_PX_PER_SEC - FloatingButtonService.MIN_FLING_TRIGGER_PX_PER_SEC) /
                    FloatingButtonService.FLING_TRIGGER_STEP_PX_PER_SEC - 1
                ).coerceAtLeast(0)
        )
    }
}

@Composable
private fun FloatingButtonSliderCard(
    title: String,
    valueText: String,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(valueText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            )
        }
    }
}

@Composable
private fun FloatingButtonPreview(alpha: Float, outerDp: Int, iconDp: Int) {
    val effectiveIconDp = iconDp.coerceAtMost(outerDp)
    val context = LocalContext.current
    val iconBitmap = produceState<ImageBitmap?>(initialValue = null, key1 = context.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(context.packageName)
                val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
                val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
                val bm = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                val cv = android.graphics.Canvas(bm)
                drawable.setBounds(0, 0, cv.width, cv.height)
                drawable.draw(cv)
                bm.asImageBitmap()
            }.getOrNull()
        }
    }.value
    Box(
        modifier = Modifier
            .size(outerDp.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(effectiveIconDp.dp)
            )
        } else {
            Icon(
                Icons.Rounded.Apps,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(effectiveIconDp.dp)
            )
        }
    }
}

@Composable
private fun DeleteDropTarget(
    hovered: Boolean,
    onBoundsChange: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (hovered) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (hovered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            val size = coords.size
            onBoundsChange(
                androidx.compose.ui.geometry.Rect(
                    pos.x,
                    pos.y,
                    pos.x + size.width,
                    pos.y + size.height
                )
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "Delete",
                tint = if (hovered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Delete",
                color = if (hovered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePageCanvas(
    page: PageLayout,
    pageIndex: Int,
    grid: GridSpec,
    appIndex: Map<String, AppCatalogEntry>,
    iconRepository: LauncherIconRepository,
    activity: MainActivity,
    appWidgetHost: android.appwidget.AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    editMode: Boolean,
    selectedEmptyTarget: EmptyTarget?,
    onSelectEmpty: (EmptyTarget) -> Unit,
    onRequestEditMode: () -> Unit,
    onActivateItem: (HomeItem, FolderOpenAnchor?) -> Unit,
    onLongPressItem: (ItemTarget) -> Unit,
    onMoveItem: (String, Int, Int, Int, Boolean, Boolean, Offset) -> Unit,
    onEdgePageRequest: (Int) -> Boolean = { false },
    onGridLayoutChange: (HomeGridInfo) -> Unit = {},
    onDragGhostChange: (PageDragGhost?) -> Unit = {},
    selectedItemIds: Set<String> = emptySet(),
    onToggleSelect: (String) -> Unit = {},
    onBulkMove: (Set<String>, String, Int, Int, Int, Boolean, String?) -> Unit = { _, _, _, _, _, _, _ -> },
    onPeekItem: (ItemTarget) -> Unit = {},
    onDismissPeek: () -> Unit = {},
    deleteDropBounds: androidx.compose.ui.geometry.Rect? = null,
    onDragStateChange: (Boolean) -> Unit = {},
    onDeleteHoverPositionChange: (Offset?) -> Unit = {},
    onDeleteItems: (Int, Set<String>) -> Unit = { _, _ -> },
    onResizeWidget: (String, Int, Int) -> Unit = { _, _, _ -> },
    isLocked: Boolean = false
) {
    val density = LocalDensity.current
    val spacing = if (editMode) 3.dp else 1.dp
    var draggingItemId by remember(page.index, editMode) { mutableStateOf<String?>(null) }
    var dragOffset by remember(page.index, editMode) { mutableStateOf(Offset.Zero) }
    var dragTouchOffsetInItem by remember(page.index, editMode) { mutableStateOf(Offset.Zero) }
    var dragStartedOutsideEdit by remember(page.index, editMode) { mutableStateOf(false) }
    var hoveredItemId by remember(page.index, editMode) { mutableStateOf<String?>(null) }
    var insertTargetId by remember(page.index, editMode) { mutableStateOf<String?>(null) }
    var insertSide by remember(page.index, editMode) { mutableIntStateOf(0) }
    var pushedItemIds by remember(page.index, editMode) { mutableStateOf<Set<String>>(emptySet()) }
    var pushDirection by remember(page.index, editMode) { mutableIntStateOf(1) }
    var pageOriginInWindow by remember(page.index) { mutableStateOf(Offset.Zero) }
    var dragPageOriginInWindow by remember(page.index, editMode) { mutableStateOf(Offset.Zero) }
    val dragScope = rememberCoroutineScope()
    val currentDeleteDropBounds by rememberUpdatedState(deleteDropBounds)
    var edgeHoverJob by remember(page.index, editMode) { mutableStateOf<Job?>(null) }
    var edgeHoverDirection by remember(page.index, editMode) { mutableIntStateOf(0) }
    var edgeNavigationDelta by remember(page.index, editMode) { mutableIntStateOf(0) }
    val highlightColor = MaterialTheme.colorScheme.primary
    val insertColor = MaterialTheme.colorScheme.tertiary

    fun resetDragState() {
        edgeHoverJob?.cancel()
        edgeHoverJob = null
        edgeHoverDirection = 0
        edgeNavigationDelta = 0
        draggingItemId = null
        dragOffset = Offset.Zero
        dragTouchOffsetInItem = Offset.Zero
        dragStartedOutsideEdit = false
        hoveredItemId = null
        insertTargetId = null
        insertSide = 0
        pushedItemIds = emptySet()
        onDragGhostChange(null)
        onDeleteHoverPositionChange(null)
        onDragStateChange(false)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                pageOriginInWindow = pos
                val size = coords.size
                if (size.width > 0 && size.height > 0) {
                    val cellW = (size.width - with(density) { spacing.toPx() } * (grid.cols - 1)) / grid.cols
                    val cellH = (size.height - with(density) { spacing.toPx() } * (grid.rows - 1)) / grid.rows
                    val sp = with(density) { spacing.toPx() }
                    onGridLayoutChange(
                        HomeGridInfo(
                            pageIndex = pageIndex,
                            originXPx = pos.x,
                            originYPx = pos.y,
                            cellStepXPx = cellW + sp,
                            cellStepYPx = cellH + sp,
                            cols = grid.cols,
                            rows = grid.rows
                        )
                    )
                }
            }
    ) {
        val cellWidth = (maxWidth - spacing * (grid.cols - 1)) / grid.cols
        val cellHeight = (maxHeight - spacing * (grid.rows - 1)) / grid.rows
        val cellStepXPx = with(density) { (cellWidth + spacing).toPx() }
        val cellStepYPx = with(density) { (cellHeight + spacing).toPx() }
        val pageWidthPx = with(density) { maxWidth.toPx() }
        val pageHeightPx = with(density) { maxHeight.toPx() }

        Box(modifier = Modifier.fillMaxSize()) {
            repeat(grid.rows) { y ->
                repeat(grid.cols) { x ->
                    val isSelected = selectedEmptyTarget?.pageIndex == pageIndex && selectedEmptyTarget.x == x && selectedEmptyTarget.y == y
                    Box(
                        modifier = Modifier
                            .offset(x = (cellWidth + spacing) * x.toFloat(), y = (cellHeight + spacing) * y.toFloat())
                            .size(cellWidth, cellHeight)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (editMode) {
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .combinedClickable(
                                enabled = editMode,
                                onClick = { onSelectEmpty(EmptyTarget(pageIndex, x, y)) },
                                onLongClick = {}
                            )
                    )
                }
            }

            page.items.forEach { item ->
                val width = cellWidth * item.spanX.toFloat() + spacing * (item.spanX - 1).toFloat()
                val height = cellHeight * item.spanY.toFloat() + spacing * (item.spanY - 1).toFloat()
                val baseX = (cellWidth + spacing) * item.x.toFloat()
                val baseY = (cellHeight + spacing) * item.y.toFloat()
                val itemDragOffset = if (draggingItemId == item.id) dragOffset else Offset.Zero
                val isDragging = draggingItemId == item.id
                val isHovered = hoveredItemId == item.id && !isDragging
                val isInsertTarget = insertTargetId == item.id && !isDragging
                val isSelected = selectedItemIds.contains(item.id)
                val isInDragGroup = isDragging && selectedItemIds.contains(item.id) && selectedItemIds.size > 1
                val isFollower = !isDragging && draggingItemId != null && selectedItemIds.contains(item.id) && selectedItemIds.contains(draggingItemId!!)
                val followerOffset = if (isFollower) dragOffset else Offset.Zero
                val pushPreviewPx = if (!isDragging && !isFollower && pushedItemIds.contains(item.id)) {
                    androidx.compose.animation.core.animateFloatAsState(
                        targetValue = cellStepXPx * pushDirection,
                        label = "push-${item.id}"
                    ).value
                } else 0f

                // Let AppWidgetHostView receive taps when not editing; parent pointerInput would steal all touches.
                val widgetPassthroughTouches = item.type == HomeItemTypes.WIDGET && !editMode

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = with(density) { baseX.toPx() }.roundToInt() + itemDragOffset.x.roundToInt() + followerOffset.x.roundToInt() + pushPreviewPx.roundToInt(),
                                y = with(density) { baseY.toPx() }.roundToInt() + itemDragOffset.y.roundToInt() + followerOffset.y.roundToInt()
                            )
                        }
                        .size(width, height)
                        .graphicsLayer {
                            alpha = when {
                                isDragging -> 0f
                                isFollower -> 0.85f
                                else -> 1f
                            }
                            val scale = when {
                                isDragging -> 1.10f
                                isHovered -> 1.08f
                                isFollower -> 1.05f
                                else -> if (isSelected) 0.94f else 1f
                            }
                            scaleX = scale
                            scaleY = scale
                        }
                        .then(
                            if (widgetPassthroughTouches) {
                                Modifier
                            } else {
                                Modifier.pointerInput(editMode, item.id, item.type, item.x, item.y, item.appRefs.size, isLocked, selectedItemIds) {
                                    detectTapOrLongPressDrag(
                                onTap = {
                                    val centerInWindow = Offset(
                                        pageOriginInWindow.x + with(density) { baseX.toPx() } + with(density) { width.toPx() } / 2f,
                                        pageOriginInWindow.y + with(density) { baseY.toPx() } + with(density) { height.toPx() } / 2f
                                    )
                                    val openAnchor = FolderOpenAnchor(
                                        centerYInWindowPx = centerInWindow.y,
                                        sourceHeightPx = with(density) { height.toPx() }
                                    )
                                    if (!editMode) {
                                        onActivateItem(item, openAnchor)
                                    } else if (item.type == HomeItemTypes.FOLDER) {
                                        onActivateItem(item, openAnchor)
                                    } else {
                                        onToggleSelect(item.id)
                                    }
                                },
                                onLongPressStart = { touchOffset ->
                                    val canDrag = editMode || !isLocked
                                    if (canDrag) {
                                        draggingItemId = item.id
                                        dragOffset = Offset.Zero
                                        dragTouchOffsetInItem = touchOffset
                                        dragPageOriginInWindow = pageOriginInWindow
                                        dragStartedOutsideEdit = !editMode
                                        hoveredItemId = null
                                        insertTargetId = null
                                        insertSide = 0
                                        pushedItemIds = emptySet()
                                        onDragStateChange(true)
                                        onDragGhostChange(
                                            PageDragGhost(
                                                item = item.deepCopy(),
                                                centerInWindow = Offset(
                                                    dragPageOriginInWindow.x + with(density) { baseX.toPx() } + touchOffset.x,
                                                    dragPageOriginInWindow.y + with(density) { baseY.toPx() } + touchOffset.y
                                                ),
                                                widthPx = with(density) { width.toPx() },
                                                heightPx = with(density) { height.toPx() }
                                            )
                                        )
                                        onDeleteHoverPositionChange(
                                            Offset(
                                                dragPageOriginInWindow.x + with(density) { baseX.toPx() } + touchOffset.x,
                                                dragPageOriginInWindow.y + with(density) { baseY.toPx() } + touchOffset.y
                                            )
                                        )
                                    }
                                    onPeekItem(ItemTarget(Area.PAGE, pageIndex, item.id))
                                },
                                onDragEnd = {
                                    val moveThresholdPx = with(density) { 10.dp.toPx() }
                                    val actuallyDragged = dragOffset.getDistance() > moveThresholdPx
                                    val wasOutsideEdit = dragStartedOutsideEdit
                                    if (!actuallyDragged) {
                                        resetDragState()
                                        // Peek already shown at onLongPressStart; keep it open
                                        return@detectTapOrLongPressDrag
                                    }
                                    onDismissPeek()
                                    // Locked + not in edit mode: skip moving, just clean up
                                    if (isLocked && !editMode) {
                                        resetDragState()
                                        return@detectTapOrLongPressDrag
                                    }
                                    val baseXPx = with(density) { baseX.toPx() }
                                    val baseYPx = with(density) { baseY.toPx() }
                                    val widthPx = with(density) { width.toPx() }
                                    val heightPx = with(density) { height.toPx() }
                                    val pointerInPage = Offset(
                                        baseXPx + dragTouchOffsetInItem.x + dragOffset.x,
                                        baseYPx + dragTouchOffsetInItem.y + dragOffset.y
                                    )
                                    val dropScreenPos = dragPageOriginInWindow + pointerInPage
                                    val rawX = (pointerInPage.x - widthPx / 2f) / cellStepXPx
                                    val rawY = (pointerInPage.y - heightPx / 2f) / cellStepYPx
                                    var targetX = rawX.roundToInt().coerceIn(0, grid.cols - item.spanX)
                                    val targetY = rawY.roundToInt().coerceIn(0, grid.rows - item.spanY)
                                    val pageDelta = when {
                                        pointerInPage.x < cellStepXPx * 0.22f -> -1
                                        pointerInPage.x > pageWidthPx - cellStepXPx * 0.22f -> 1
                                        else -> 0
                                    }
                                    val effectivePageDelta = edgeNavigationDelta.takeIf { it != 0 } ?: pageDelta
                                    val sendToDock = pointerInPage.y > pageHeightPx - cellStepYPx * 0.35f
                                    val didMerge = hoveredItemId != null
                                    val didInsert = insertTargetId != null && !didMerge
                                    if (didInsert && insertSide > 0) {
                                        targetX = (targetX + 1).coerceIn(0, grid.cols - item.spanX)
                                    }
                                    val hovered = hoveredItemId
                                    val multiSelectIds = selectedItemIds
                                    val shouldDelete = currentDeleteDropBounds?.contains(dropScreenPos) == true
                                    resetDragState()
                                    if (shouldDelete) {
                                        val idsToDelete = if (multiSelectIds.size > 1 && multiSelectIds.contains(item.id)) {
                                            multiSelectIds
                                        } else {
                                            setOf(item.id)
                                        }
                                        onDeleteItems(pageIndex, idsToDelete)
                                        if (wasOutsideEdit) onRequestEditMode()
                                        return@detectTapOrLongPressDrag
                                    }
                                    if (multiSelectIds.size > 1 && multiSelectIds.contains(item.id)) {
                                        onBulkMove(multiSelectIds, item.id, targetX, targetY, if (sendToDock) 0 else effectivePageDelta, sendToDock, hovered)
                                    } else {
                                        onMoveItem(item.id, targetX, targetY, if (sendToDock) 0 else effectivePageDelta, sendToDock, didInsert, dropScreenPos)
                                    }
                                    if (wasOutsideEdit) onRequestEditMode()
                                },
                                onDragCancel = {
                                    resetDragState()
                                },
                                onDrag = { _, amount ->
                                    dragOffset += amount
                                    if (dragOffset.getDistance() > with(density) { 10.dp.toPx() }) {
                                        onDismissPeek()
                                    }
                                    val baseXPx = with(density) { baseX.toPx() }
                                    val baseYPx = with(density) { baseY.toPx() }
                                    val pointerInPage = Offset(
                                        baseXPx + dragTouchOffsetInItem.x + dragOffset.x,
                                        baseYPx + dragTouchOffsetInItem.y + dragOffset.y
                                    )
                                    val pointerScreenPos = dragPageOriginInWindow + pointerInPage
                                    onDragGhostChange(
                                        PageDragGhost(
                                            item = item.deepCopy(),
                                            centerInWindow = pointerScreenPos,
                                            widthPx = with(density) { width.toPx() },
                                            heightPx = with(density) { height.toPx() }
                                        )
                                    )
                                    val edgeZonePx = with(density) { 36.dp.toPx() }
                                    val requestedEdgeDirection = when {
                                        pointerInPage.x <= edgeZonePx -> -1
                                        pointerInPage.x >= pageWidthPx - edgeZonePx -> 1
                                        else -> 0
                                    }
                                    if (requestedEdgeDirection != edgeHoverDirection) {
                                        edgeHoverJob?.cancel()
                                        edgeHoverJob = null
                                        edgeHoverDirection = requestedEdgeDirection
                                        if (requestedEdgeDirection != 0) {
                                            edgeHoverJob = dragScope.launch {
                                                delay(650)
                                                if (onEdgePageRequest(requestedEdgeDirection)) {
                                                    edgeNavigationDelta += requestedEdgeDirection
                                                }
                                                edgeHoverDirection = 0
                                                edgeHoverJob = null
                                            }
                                        }
                                    }
                                    onDeleteHoverPositionChange(pointerScreenPos)
                                    val pointerCellX = (pointerInPage.x / cellStepXPx).toInt().coerceIn(0, grid.cols - 1)
                                    val pointerCellY = (pointerInPage.y / cellStepYPx).toInt().coerceIn(0, grid.rows - 1)
                                    val candidate = if (item.type == HomeItemTypes.APP && item.spanX == 1 && item.spanY == 1) {
                                        page.items.firstOrNull { c ->
                                            c.id != item.id && c.spanX == 1 && c.spanY == 1 &&
                                                (c.type == HomeItemTypes.APP || c.type == HomeItemTypes.FOLDER) &&
                                                pointerCellX in c.x until (c.x + c.spanX) &&
                                                pointerCellY in c.y until (c.y + c.spanY)
                                        }
                                    } else null
                                    if (candidate != null) {
                                        val candidateCenterX = (candidate.x + 0.5f) * cellStepXPx
                                        val distanceFromCenter = kotlin.math.abs(pointerInPage.x - candidateCenterX) / cellStepXPx
                                        if (candidate.type == HomeItemTypes.FOLDER || distanceFromCenter < 0.28f) {
                                            hoveredItemId = candidate.id
                                            insertTargetId = null
                                            insertSide = 0
                                            pushedItemIds = emptySet()
                                        } else {
                                            hoveredItemId = null
                                            insertTargetId = candidate.id
                                            insertSide = if (pointerInPage.x > candidateCenterX) 1 else -1
                                            val insertX = if (insertSide > 0) candidate.x + 1 else candidate.x
                                            val preview = computePushPreview(page, item.id, insertX, pointerCellY, grid.cols)
                                            pushedItemIds = preview.ids
                                            pushDirection = preview.direction
                                        }
                                    } else {
                                        hoveredItemId = null
                                        insertTargetId = null
                                        insertSide = 0
                                        pushedItemIds = emptySet()
                                    }
                                }
                            )
                        }
                            }
                    )
                ) {
                    if (isHovered) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                                .background(highlightColor.copy(alpha = 0.22f))
                        )
                    }
                    if (isInsertTarget && insertSide != 0) {
                        Box(
                            modifier = Modifier
                                .align(if (insertSide > 0) Alignment.CenterEnd else Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(4.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(insertColor.copy(alpha = 0.9f))
                        )
                    }
                    when (item.type) {
                        HomeItemTypes.FOLDER -> FolderTile(item, appIndex, iconRepository, large = item.spanY > 1 || item.spanX > 1)
                        HomeItemTypes.WIDGET -> WidgetTile(activity, appWidgetHost, appWidgetManager, item)
                        HomeItemTypes.SHORTCUT -> ShortcutTile(item, large = item.spanY > 1 || item.spanX > 1)
                        else -> AppTile(item, appIndex, iconRepository, large = item.spanY > 1 || item.spanX > 1)
                    }
                    if (isHovered) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                                .border(3.dp, highlightColor.copy(alpha = 0.95f), MaterialTheme.shapes.medium)
                        )
                    }
                    if (editMode && item.type == HomeItemTypes.WIDGET && isWidgetResizable(appWidgetManager, item) && !isDragging) {
                        WidgetResizeHandles(
                            item = item,
                            grid = grid,
                            cellStepXPx = cellStepXPx,
                            cellStepYPx = cellStepYPx,
                            isAreaFree = { sx, sy -> page.isAreaFree(item.x, item.y, sx, sy, item.id) },
                            onResize = { newSpanX, newSpanY -> onResizeWidget(item.id, newSpanX, newSpanY) },
                            handleColor = highlightColor
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                                .border(2.dp, highlightColor, MaterialTheme.shapes.medium)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(20.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(highlightColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTile(item: HomeItem, appIndex: Map<String, AppCatalogEntry>, iconRepository: LauncherIconRepository, large: Boolean) {
    val app = appIndex["${item.packageName}/${item.activityName}"]
    val icon = rememberAppIcon(iconRepository, app)
    TileShell(
        background = Color.Transparent,
        borderAlpha = 0f,
        padding = if (large) 1.dp else 0.dp
    ) {
        IconAndLabel(icon = icon, label = item.labelOrFallback(appIndex), compact = !large)
    }
}

@Composable
private fun ShortcutTile(item: HomeItem, large: Boolean) {
    val iconPath = item.shortcutIconPath
    val iconBitmap = androidx.compose.runtime.produceState<ImageBitmap?>(initialValue = null, key1 = iconPath) {
        value = if (iconPath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(iconPath)?.asImageBitmap() }.getOrNull()
            }
        }
    }.value
    TileShell(
        background = Color.Transparent,
        borderAlpha = 0f,
        padding = if (large) 1.dp else 0.dp
    ) {
        IconAndLabel(icon = iconBitmap, label = item.title ?: "Shortcut", compact = !large)
    }
}

@Composable
private fun FolderTile(
    item: HomeItem,
    appIndex: Map<String, AppCatalogEntry>,
    iconRepository: LauncherIconRepository,
    large: Boolean
) {
    val previewApps = folderPreviewApps(item, appIndex)
    val folderLabel = item.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Folder"
    val compact = !large
    val folderIconSize = if (compact) 60.dp else 80.dp
    val miniIconSize = if (compact) 24.dp else 28.dp
    TileShell(
        background = Color.Transparent,
        borderAlpha = 0f,
        padding = if (large) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(0.dp))
            Box(
                modifier = Modifier
                    .size(folderIconSize)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)) {
                    previewApps.chunked(2).forEach { columnApps ->
                        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)) {
                            columnApps.forEach { app ->
                                val icon = rememberAppIcon(iconRepository, app)
                                IconBadge(icon, size = miniIconSize)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                formatLabelForTile(folderLabel),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = TextAlign.Center,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, lineHeight = 15.sp)
                } else {
                    MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, lineHeight = 17.sp)
                },
                color = Color.White
            )
        }
    }
}

@Composable
private fun WidgetTile(
    activity: MainActivity,
    appWidgetHost: android.appwidget.AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    item: HomeItem
) {
    val hostView = remember(item.appWidgetId) { makeWidgetHostView(activity, appWidgetHost, appWidgetManager, item) }
    // No Card / no padding / no background tint so the widget renders flush against the wallpaper.
    Box(modifier = Modifier.fillMaxSize()) {
        if (hostView != null) {
            AndroidView(factory = { hostView }, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(item.title ?: "Widget unavailable", textAlign = TextAlign.Center, color = Color.White)
            }
        }
    }
}

@Composable
private fun BoxScope.WidgetResizeHandles(
    item: HomeItem,
    grid: GridSpec,
    cellStepXPx: Float,
    cellStepYPx: Float,
    isAreaFree: (Int, Int) -> Boolean,
    onResize: (Int, Int) -> Unit,
    handleColor: Color
) {
    var dragDx by remember(item.id) { mutableStateOf(0f) }
    var dragDy by remember(item.id) { mutableStateOf(0f) }
    val maxSpanX = grid.cols - item.x
    val maxSpanY = grid.rows - item.y

    fun previewSpan(): Pair<Int, Int> {
        val deltaX = (dragDx / cellStepXPx).roundToInt()
        val deltaY = (dragDy / cellStepYPx).roundToInt()
        val newX = (item.spanX + deltaX).coerceIn(1, maxSpanX)
        val newY = (item.spanY + deltaY).coerceIn(1, maxSpanY)
        return newX to newY
    }

    val (previewX, previewY) = previewSpan()
    val previewValid = isAreaFree(previewX, previewY)

    // Visual border showing widget bounds (so user knows it's resizable)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                BorderStroke(1.dp, handleColor.copy(alpha = if (previewValid) 0.55f else 0.3f)),
                MaterialTheme.shapes.medium
            )
    )

    val handleModifierBase = Modifier
        .size(22.dp)
        .clip(androidx.compose.foundation.shape.CircleShape)
        .background(handleColor.copy(alpha = 0.95f))

    // Right-edge handle (resize horizontal)
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .offset(x = 11.dp)
            .then(handleModifierBase)
            .pointerInput(item.id, item.spanX, item.spanY) {
                detectDragGestures(
                    onDragEnd = {
                        val (nx, ny) = previewSpan()
                        if ((nx != item.spanX) && isAreaFree(nx, item.spanY)) {
                            onResize(nx, item.spanY)
                        }
                        dragDx = 0f; dragDy = 0f
                    },
                    onDragCancel = { dragDx = 0f; dragDy = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDx += amount.x
                    }
                )
            }
    )

    // Bottom-edge handle (resize vertical)
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = 11.dp)
            .then(handleModifierBase)
            .pointerInput(item.id, item.spanX, item.spanY) {
                detectDragGestures(
                    onDragEnd = {
                        val (nx, ny) = previewSpan()
                        if ((ny != item.spanY) && isAreaFree(item.spanX, ny)) {
                            onResize(item.spanX, ny)
                        }
                        dragDx = 0f; dragDy = 0f
                    },
                    onDragCancel = { dragDx = 0f; dragDy = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDy += amount.y
                    }
                )
            }
    )

    // Bottom-right corner handle (resize both)
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 11.dp, y = 11.dp)
            .then(handleModifierBase)
            .pointerInput(item.id, item.spanX, item.spanY) {
                detectDragGestures(
                    onDragEnd = {
                        val (nx, ny) = previewSpan()
                        if ((nx != item.spanX || ny != item.spanY) && isAreaFree(nx, ny)) {
                            onResize(nx, ny)
                        }
                        dragDx = 0f; dragDy = 0f
                    },
                    onDragCancel = { dragDx = 0f; dragDy = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDx += amount.x
                        dragDy += amount.y
                    }
                )
            }
    )
}

@Composable
private fun TileShell(
    padding: Dp = 10.dp,
    background: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    borderAlpha: Float = 0.55f,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = background),
        border = if (borderAlpha > 0f) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
private fun IconAndLabel(icon: ImageBitmap?, label: String, compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(0.dp))
        IconBadge(icon, if (compact) 60.dp else 80.dp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            formatLabelForTile(label),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = if (compact) {
                MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp)
            } else {
                MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 14.sp)
            },
            color = Color.White
        )
    }
}

@Composable
private fun DockIconTile(item: HomeItem, appIndex: Map<String, AppCatalogEntry>, iconRepository: LauncherIconRepository) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (item.type == HomeItemTypes.FOLDER) {
            val previewApps = folderPreviewApps(item, appIndex)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    previewApps.chunked(2).forEach { columnApps ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            columnApps.forEach { app ->
                                val icon = rememberAppIcon(iconRepository, app)
                                IconBadge(icon, size = 14.dp)
                            }
                        }
                    }
                }
            }
        } else {
            val app = appIndex["${item.packageName}/${item.activityName}"]
            val icon = rememberAppIcon(iconRepository, app)
            IconBadge(icon, 46.dp)
        }
    }
}

@Composable
private fun IconBadge(icon: ImageBitmap?, size: Dp) {
    if (icon != null) {
        androidx.compose.foundation.Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Apps, contentDescription = null)
        }
    }
}

@Composable
private fun RecentAppsOverlay(
    recentApps: List<AppCatalogEntry>,
    iconRepository: LauncherIconRepository,
    onLaunch: (AppCatalogEntry) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(onClick = onDismiss)
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.52f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "最近使用",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (recentApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "從啟動器開啟 App 後，最近使用的 App 會顯示在這裡",
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = 4
                        val rows = 3
                        val spacing = 8.dp
                        val tileWidth = (maxWidth - spacing * (columns - 1)) / columns
                        val tileHeight = (maxHeight - spacing * (rows - 1)) / rows
                        recentApps.take(columns * rows).forEachIndexed { index, app ->
                            val x = index % columns
                            val y = index / columns
                            val icon = rememberAppIcon(iconRepository, app)
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (tileWidth + spacing) * x.toFloat(),
                                        y = (tileHeight + spacing) * y.toFloat()
                                    )
                                    .size(tileWidth, tileHeight)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onLaunch(app) }
                                    .padding(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    IconBadge(icon, 58.dp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatLabelForTile(app.label),
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(count: Int, currentPage: Int, homePage: Int) {
    val activeColor = MaterialTheme.colorScheme.primary
    val homeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count.coerceAtLeast(1)) { index ->
            val isCurrent = index == currentPage
            val isHome = index == homePage
            val width = when {
                isCurrent -> 18.dp
                isHome -> 12.dp
                else -> 8.dp
            }
            val color = when {
                isCurrent -> activeColor
                isHome -> homeColor
                else -> inactiveColor
            }
            Canvas(modifier = Modifier.padding(horizontal = 4.dp).size(width = width, height = 8.dp)) {
                drawRoundRect(
                    color = color,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2, size.height / 2)
                )
            }
        }
    }
}

@Composable
private fun RecentAppsDockRow(
    modifier: Modifier = Modifier,
    recentApps: List<AppCatalogEntry>,
    iconRepository: LauncherIconRepository,
    onLaunch: (AppCatalogEntry) -> Unit,
    onPressedChange: (Boolean) -> Unit,
    onBoundsChange: (RectF?) -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val recentKeys = remember(recentApps) { recentApps.map { it.key } }
    LaunchedEffect(recentKeys, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    Card(
        modifier = modifier.onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            val topPaddingPx = with(density) { 24.dp.toPx() }
            onBoundsChange(RectF(0f, pos.y - topPaddingPx, 100_000f, 100_000f))
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                onPressedChange(event.changes.any { it.pressed })
                            }
                        }
                    }
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                recentApps.take(MAX_RECENT_APPS).forEach { app ->
                    RecentDockIcon(app = app, iconRepository = iconRepository, onLaunch = onLaunch)
                }
            }
            if (scrollState.maxValue > 0) {
                Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    val trackHeight = size.height
                    val thumbWidth = (size.width * 0.28f).coerceAtLeast(trackHeight)
                    val maxTravel = (size.width - thumbWidth).coerceAtLeast(0f)
                    val progress = scrollState.value / scrollState.maxValue.toFloat()
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.14f),
                        size = Size(size.width, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.48f),
                        topLeft = Offset(maxTravel * progress, 0f),
                        size = Size(thumbWidth, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentDockIcon(
    app: AppCatalogEntry,
    iconRepository: LauncherIconRepository,
    onLaunch: (AppCatalogEntry) -> Unit
) {
    val icon = rememberAppIcon(iconRepository, app)
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onLaunch(app) },
        contentAlignment = Alignment.Center
    ) {
        IconBadge(icon, 40.dp)
    }
}

@Composable
private fun DockBar(
    modifier: Modifier = Modifier,
    dockItems: List<HomeItem>,
    appIndex: Map<String, AppCatalogEntry>,
    iconRepository: LauncherIconRepository,
    editMode: Boolean,
    onRequestEditMode: () -> Unit,
    onMoveItem: (String, Int) -> Unit,
    onMoveToPage: (String, Offset) -> Unit,
    onActivate: (HomeItem, FolderOpenAnchor?) -> Unit,
    onLongPress: (HomeItem) -> Unit,
    onPeek: (HomeItem) -> Unit = {},
    onDismissPeek: () -> Unit = {},
    onDockLayoutChange: (DockLayoutInfo) -> Unit = {},
    deleteDropBounds: androidx.compose.ui.geometry.Rect? = null,
    onDragStateChange: (Boolean) -> Unit = {},
    onDeleteHoverPositionChange: (Offset?) -> Unit = {},
    onDeleteDockItem: (String) -> Unit = {},
    isLocked: Boolean = false
) {
    val density = LocalDensity.current
    val dockTilePositions = remember(editMode, dockItems) { mutableStateMapOf<Int, Offset>() }
    var dockTopY by remember(editMode) { mutableStateOf(Float.MAX_VALUE) }
    var dockOriginX by remember(editMode) { mutableStateOf(0f) }
    var dockSlotStepPx by remember(editMode) { mutableStateOf(0f) }
    var draggingDockId by remember(editMode, dockItems) { mutableStateOf<String?>(null) }
    var dragOffset by remember(editMode, dockItems) { mutableStateOf(Offset.Zero) }
    var dragTouchOffsetInDockItem by remember(editMode, dockItems) { mutableStateOf(Offset.Zero) }
    var dragStartedOutsideEdit by remember(editMode, dockItems) { mutableStateOf(false) }
    val currentDeleteDropBounds by rememberUpdatedState(deleteDropBounds)

    fun resetDockDrag() {
        draggingDockId = null
        dragOffset = Offset.Zero
        dragTouchOffsetInDockItem = Offset.Zero
        dragStartedOutsideEdit = false
        onDeleteHoverPositionChange(null)
        onDragStateChange(false)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (editMode) 0.84f else 0.38f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (editMode) 1f else 0.35f)),
        modifier = modifier.onGloballyPositioned { coords ->
            dockTopY = coords.positionInWindow().y
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    val sz = coords.size
                    if (sz.width > 0) {
                        val slotStep = sz.width.toFloat() / DOCK_SLOTS
                        dockOriginX = pos.x
                        dockSlotStepPx = slotStep
                        onDockLayoutChange(DockLayoutInfo(pos.x, pos.y, slotStep, DOCK_SLOTS))
                    }
                }
        ) {
            val slotWidth = (maxWidth - 8.dp * (DOCK_SLOTS - 1)) / DOCK_SLOTS
            val stepPx = with(density) { (slotWidth + 8.dp).toPx() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(DOCK_SLOTS) { index ->
                    val item = dockItems.dockItemAt(index)
                    Box(modifier = Modifier.width(slotWidth).requiredHeight(54.dp)) {
                        if (item == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (editMode) 0.35f else 0.2f))
                            )
                        } else {
                            val isDraggingDock = draggingDockId == item.id
                            val dockTileSizePx = with(density) { 54.dp.toPx() }
                            val itemDragX = if (isDraggingDock) {
                                (dragOffset.x + dragTouchOffsetInDockItem.x - dockTileSizePx / 2f).roundToInt()
                            } else 0
                            val itemDragY = if (isDraggingDock) {
                                (dragOffset.y + dragTouchOffsetInDockItem.y - dockTileSizePx / 2f).roundToInt()
                            } else 0
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(itemDragX, itemDragY) }
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = if (isDraggingDock) 0.92f else 1f
                                        val scale = if (isDraggingDock) 1.10f else 1f
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .onGloballyPositioned { coords ->
                                        if (!isDraggingDock) {
                                            dockTilePositions[index] = coords.positionInWindow()
                                        }
                                    }
                                    .pointerInput(editMode, item.id, item.type, index, item.appRefs.size, isLocked) {
                                        detectTapOrLongPressDrag(
                                            onTap = {
                                                val tilePos = dockTilePositions[index]
                                                val openAnchor = tilePos?.let {
                                                    FolderOpenAnchor(
                                                        centerYInWindowPx = it.y + dockTileSizePx / 2f,
                                                        sourceHeightPx = dockTileSizePx
                                                    )
                                                }
                                                if (editMode && item.type != HomeItemTypes.FOLDER) {
                                                    onLongPress(item)
                                                } else {
                                                    onActivate(item, openAnchor)
                                                }
                                            },
                                            onLongPressStart = { touchOffset ->
                                                val canDrag = editMode || !isLocked
                                                if (canDrag) {
                                                    draggingDockId = item.id
                                                    dragOffset = Offset.Zero
                                                    dragTouchOffsetInDockItem = touchOffset
                                                    dragStartedOutsideEdit = !editMode
                                                    onDragStateChange(true)
                                                    val tilePos = dockTilePositions[index] ?: Offset.Zero
                                                    val pointerScreenPos = Offset(
                                                        tilePos.x + touchOffset.x,
                                                        tilePos.y + touchOffset.y
                                                    )
                                                    onDeleteHoverPositionChange(pointerScreenPos)
                                                }
                                                onPeek(item)
                                            },
                                            onDragEnd = {
                                                val moveThresholdPx = with(density) { 10.dp.toPx() }
                                                val actuallyDragged = dragOffset.getDistance() > moveThresholdPx
                                                val wasOutsideEdit = dragStartedOutsideEdit
                                                if (!actuallyDragged) {
                                                    resetDockDrag()
                                                    return@detectTapOrLongPressDrag
                                                }
                                                onDismissPeek()
                                                if (isLocked && !editMode) {
                                                    resetDockDrag()
                                                    return@detectTapOrLongPressDrag
                                                }
                                                val tilePos = dockTilePositions[index] ?: Offset.Zero
                                                val finalPointer = tilePos + dragTouchOffsetInDockItem + dragOffset
                                                val shouldDelete = currentDeleteDropBounds?.contains(finalPointer) == true
                                                resetDockDrag()
                                                if (shouldDelete) {
                                                    onDeleteDockItem(item.id)
                                                    if (wasOutsideEdit) onRequestEditMode()
                                                    return@detectTapOrLongPressDrag
                                                }
                                                val draggedToPage = finalPointer.y < dockTopY - with(density) { 24.dp.toPx() }
                                                if (draggedToPage) {
                                                    onMoveToPage(item.id, finalPointer)
                                                    if (wasOutsideEdit) onRequestEditMode()
                                                    return@detectTapOrLongPressDrag
                                                }
                                                val targetIndex = if (dockSlotStepPx > 0f) {
                                                    (((finalPointer.x - dockOriginX) / dockSlotStepPx).toInt()).coerceIn(0, DOCK_SLOTS - 1)
                                                } else {
                                                    (index + (dragOffset.x / stepPx).roundToInt()).coerceIn(0, DOCK_SLOTS - 1)
                                                }
                                                onMoveItem(item.id, targetIndex)
                                                if (wasOutsideEdit) onRequestEditMode()
                                            },
                                            onDragCancel = {
                                                resetDockDrag()
                                            },
                                            onDrag = { _, amount ->
                                                dragOffset += amount
                                                if (dragOffset.getDistance() > with(density) { 10.dp.toPx() }) {
                                                    onDismissPeek()
                                                }
                                                val tilePos = dockTilePositions[index] ?: Offset.Zero
                                                val pointerScreenPos = tilePos + dragTouchOffsetInDockItem + dragOffset
                                                onDeleteHoverPositionChange(pointerScreenPos)
                                            }
                                        )
                                    }
                            ) {
                                DockIconTile(item, appIndex, iconRepository)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerSheet(
    apps: List<AppCatalogEntry>,
    recentApps: List<AppCatalogEntry>,
    iconRepository: LauncherIconRepository,
    onLaunch: (AppCatalogEntry) -> Unit,
    onAddToHome: (AppCatalogEntry) -> Unit,
    onAddToDock: (AppCatalogEntry) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter {
            "${it.label} ${it.packageName} ${it.activityName}".contains(query, ignoreCase = true)
        }
    }
    val groupedApps = remember(filteredApps) { filteredApps.groupBy { it.sectionKey }.toSortedMap() }
    val letters = remember(groupedApps) { groupedApps.keys.toList() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sectionOffsets = remember(groupedApps, recentApps, query) {
        val result = mutableMapOf<String, Int>()
        var index = 0
        if (query.isBlank() && recentApps.isNotEmpty()) {
            index += 2
        }
        groupedApps.forEach { (section, sectionApps) ->
            result[section] = index
            index += 1 + sectionApps.size
        }
        result
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Search apps") },
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (query.isBlank() && recentApps.isNotEmpty()) {
                    item("recents-header") {
                        Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    item("recents-row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentApps.forEach { app -> RecentAppChip(app, iconRepository, onLaunch) }
                        }
                    }
                }

                groupedApps.forEach { (section, sectionApps) ->
                    stickyHeader(key = "header-$section") {
                        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                            Text(
                                section,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    items(sectionApps, key = { it.key }) { app ->
                        AppDrawerRow(
                            app = app,
                            iconRepository = iconRepository,
                            onLaunch = { onLaunch(app) },
                            onAddToHome = { onAddToHome(app) },
                            onAddToDock = { onAddToDock(app) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(start = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                letters.forEach { letter ->
                    TextButton(
                        onClick = { sectionOffsets[letter]?.let { index -> scope.launch { listState.animateScrollToItem(index) } } },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(letter, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentAppChip(app: AppCatalogEntry, iconRepository: LauncherIconRepository, onLaunch: (AppCatalogEntry) -> Unit) {
    val icon = rememberAppIcon(iconRepository, app)
    OutlinedButton(onClick = { onLaunch(app) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBadge(icon, 34.dp)
            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
        }
    }
}

@Composable
private fun AppDrawerRow(
    app: AppCatalogEntry,
    iconRepository: LauncherIconRepository,
    onLaunch: () -> Unit,
    onAddToHome: () -> Unit,
    onAddToDock: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val icon = rememberAppIcon(iconRepository, app)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onLaunch, onLongClick = { menuExpanded = true })
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon, 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color.White)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = {
                        menuExpanded = false
                        onLaunch()
                    })
                    DropdownMenuItem(text = { Text("Add to current page") }, onClick = {
                        menuExpanded = false
                        onAddToHome()
                    })
                    DropdownMenuItem(text = { Text("Add to dock") }, onClick = {
                        menuExpanded = false
                        onAddToDock()
                    })
                }
            }
        }
    }
}

@Composable
private fun FolderOverlay(
    item: HomeItem,
    appIndex: Map<String, AppCatalogEntry>,
    iconRepository: LauncherIconRepository,
    editMode: Boolean,
    wallpaperBitmap: ImageBitmap?,
    openAnchor: FolderOpenAnchor?,
    onReorder: (MutableList<AppRef>) -> Unit,
    onDismiss: () -> Unit,
    onLaunch: (AppCatalogEntry) -> Unit,
    onExtractAt: (AppRef, Offset) -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    deleteDropBounds: androidx.compose.ui.geometry.Rect? = null,
    onDragStateChange: (Boolean) -> Unit = {},
    onDeleteHoverPositionChange: (Offset?) -> Unit = {},
    onDeleteFolderApp: (AppRef) -> Unit = {},
    onShowAppMenu: (AppRef, AppCatalogEntry) -> Unit = { _, _ -> }
) {
    val apps = resolveFolderApps(item, appIndex)
    val density = LocalDensity.current
    val currentDeleteDropBounds by rememberUpdatedState(deleteDropBounds)
    val folderLabel = item.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Folder"
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    var draggingRefKey by remember(item.id, item.appRefs) { mutableStateOf<String?>(null) }
    var dragOffset by remember(item.id, item.appRefs) { mutableStateOf(Offset.Zero) }
    var dragTouchOffsetInFolderItem by remember(item.id, item.appRefs) { mutableStateOf(Offset.Zero) }
    var extractCandidate by remember(item.id, item.appRefs) { mutableStateOf(false) }
    var contentBounds by remember(item.id) { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var overlayOriginInWindow by remember(item.id) { mutableStateOf(Offset.Zero) }
    var panelHeightPx by remember(item.id) { mutableStateOf(0f) }
    val tilePositions = remember(item.id, item.appRefs) { mutableStateMapOf<String, Offset>() }
    val appTileBoundsInWindow = remember(item.id, item.appRefs) { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val slotInfo = computeFolderSlots(apps, FOLDER_PAGE_SIZE)
    val pageCount = slotInfo.pageCount
    val hasExplicitSlot = apps.any { it.first.slot >= 0 }
    val sortedSlots = slotInfo.assignments.map { it.slot }.sorted()
    val slotsAreDenseFromZero = sortedSlots.withIndex().all { (index, slot) -> slot == index }
    val useExplicitGrid = hasExplicitSlot && !slotsAreDenseFromZero
    val folderPagerState = rememberPagerState(pageCount = { pageCount })

    fun resetFolderDrag() {
        draggingRefKey = null
        dragOffset = Offset.Zero
        dragTouchOffsetInFolderItem = Offset.Zero
        extractCandidate = false
        onDeleteHoverPositionChange(null)
        onDragStateChange(false)
    }

    fun shouldDismissForTap(localTap: Offset): Boolean {
        if (editMode) return true
        val tapInWindow = Offset(overlayOriginInWindow.x + localTap.x, overlayOriginInWindow.y + localTap.y)
        val hitAppTile = appTileBoundsInWindow.values.any { it.contains(tapInWindow) }
        return !hitAppTile
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayOriginInWindow = coords.positionInWindow()
            }
    ) {
        if (wallpaperBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(item.id, item.appRefs, editMode) {
                    detectTapGestures(onTap = { tap ->
                        if (shouldDismissForTap(tap)) onDismiss()
                    })
                }
        )

        val currentPageAssignments = slotInfo.assignments.filter {
            it.slot / FOLDER_PAGE_SIZE == folderPagerState.currentPage.coerceIn(0, pageCount - 1)
        }
        val forceFixedGrid = pageCount > 1
        val compactColumns = if (forceFixedGrid) {
            FOLDER_GRID_COLS
        } else {
            currentPageAssignments.size.coerceIn(1, FOLDER_GRID_COLS)
        }
        val compactRows = if (forceFixedGrid) {
            FOLDER_GRID_ROWS
        } else {
            ((currentPageAssignments.size + compactColumns - 1) / compactColumns)
                .coerceIn(1, FOLDER_GRID_ROWS)
        }
        val panelColumns = if (useExplicitGrid || forceFixedGrid) FOLDER_GRID_COLS else compactColumns
        val panelRows = if (useExplicitGrid || forceFixedGrid) FOLDER_GRID_ROWS else compactRows
        val folderTileWidth = 72.dp
        val folderTileHeight = 90.dp
        val folderSpacing = 5.dp
        val panelWidth = (folderTileWidth * panelColumns + folderSpacing * (panelColumns - 1) + 32.dp)
            .coerceAtMost(maxWidth - 32.dp)
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val defaultTopPx = with(density) { (maxHeight * 0.16f).toPx() }
        val estimatedAppCenterInPanelPx = run {
            if (!useExplicitGrid) {
                val headerAndGapPx = with(density) { (16.dp + 48.dp + 12.dp).toPx() }
                val estimatedPageHeightPx = with(density) { ((folderTileHeight + folderSpacing) * panelRows).toPx() }
                headerAndGapPx + estimatedPageHeightPx / 2f
            } else {
                null
            }
        }
        val appContentCenterInPanelPx = run {
            val panelBounds = contentBounds ?: return@run null
            val centers = currentPageAssignments.mapNotNull { assignment ->
                appTileBoundsInWindow[assignment.ref.key]?.let { rect ->
                    (rect.top + rect.bottom) / 2f
                }
            }
            if (centers.isEmpty()) {
                null
            } else {
                ((centers.minOrNull() ?: return@run null) + (centers.maxOrNull() ?: return@run null)) / 2f - panelBounds.top
            }
        }
        val panelTopPx = run {
            val anchor = openAnchor
            if (anchor == null || panelHeightPx <= 0f || maxHeightPx <= 0f) {
                defaultTopPx
            } else {
                val localAnchorY = anchor.centerYInWindowPx - overlayOriginInWindow.y
                val appCenterInPanelPx = appContentCenterInPanelPx ?: estimatedAppCenterInPanelPx
                val targetTop = if (appCenterInPanelPx != null) localAnchorY - appCenterInPanelPx else defaultTopPx
                val topPadding = with(density) { 20.dp.toPx() }
                val bottomPadding = with(density) { 20.dp.toPx() }
                val maxTop = (maxHeightPx - panelHeightPx - bottomPadding).coerceAtLeast(topPadding)
                targetTop.coerceIn(topPadding, maxTop)
            }
        }
        val panelTopDp = with(density) { panelTopPx.toDp() }

        Column(
            modifier = Modifier
                .width(panelWidth)
                .align(Alignment.TopCenter)
                .offset(y = panelTopDp)
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.large
                )
                .padding(16.dp)
                .graphicsLayer { alpha = if (extractCandidate) 0f else 1f }
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    val sz = coords.size
                    contentBounds = androidx.compose.ui.geometry.Rect(
                        pos.x, pos.y,
                        pos.x + sz.width, pos.y + sz.height
                    )
                    panelHeightPx = sz.height.toFloat()
                },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(folderLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editMode) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color.White)
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("Rename folder") }, onClick = {
                                    menuExpanded = false
                                    onRename()
                                })
                                DropdownMenuItem(text = { Text("Remove folder") }, onClick = {
                                    menuExpanded = false
                                    onRemove()
                                })
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
            }

            HorizontalPager(
                state = folderPagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = panelColumns
                    val rows = panelRows
                    val spacing = folderSpacing
                    val tileWidth = if (useExplicitGrid) {
                        (maxWidth - spacing * (columns - 1)) / columns
                    } else {
                        folderTileWidth.coerceAtMost((maxWidth - spacing * (columns - 1)) / columns)
                    }
                    val tileHeight = if (useExplicitGrid) tileWidth + 18.dp else folderTileHeight
                    val stepXPx = with(density) { (tileWidth + spacing).toPx() }
                    val stepYPx = with(density) { (tileHeight + spacing).toPx() }
                    val pageHeight = (tileHeight + spacing) * rows
                    Box(modifier = Modifier.fillMaxWidth().height(pageHeight)) {
                        val pageAssignments = slotInfo.assignments.filter { it.slot / FOLDER_PAGE_SIZE == pageIndex }
                        val countOnPage = pageAssignments.size
                        val applyCenter = !useExplicitGrid
                        val fullRows = countOnPage / columns
                        val itemsInLastRow = countOnPage - fullRows * columns
                        val rowsUsed = fullRows + if (itemsInLastRow > 0) 1 else 0
                        val vertOffsetCells = if (applyCenter) (rows - rowsUsed).toFloat() / 2f else 0f
                        val lastRowColOffset = if (applyCenter && itemsInLastRow > 0) (columns - itemsInLastRow).toFloat() / 2f else 0f
                        pageAssignments.forEachIndexed { localIndex, assignment ->
                            val ref = assignment.ref
                            val app = assignment.app
                            val cellInPage = assignment.slot % FOLDER_PAGE_SIZE
                            val baseCol = cellInPage % columns
                            val baseRow = cellInPage / columns
                            val rawCol: Float
                            val rawRow: Float
                            if (applyCenter) {
                                val isLastRow = localIndex >= fullRows * columns
                                rawCol = if (isLastRow) (localIndex - fullRows * columns).toFloat() + lastRowColOffset else (localIndex % columns).toFloat()
                                rawRow = (if (isLastRow) fullRows else localIndex / columns).toFloat() + vertOffsetCells
                            } else {
                                rawCol = baseCol.toFloat()
                                rawRow = baseRow.toFloat()
                            }
                            val offsetX = (tileWidth + spacing) * rawCol
                            val offsetY = (tileHeight + spacing) * rawRow
                            val refKey = ref.key
                            val itemOffset = if (draggingRefKey == refKey) dragOffset else Offset.Zero
                            val isDragging = draggingRefKey == refKey
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            with(density) { offsetX.toPx() }.roundToInt() + itemOffset.x.roundToInt(),
                                            with(density) { offsetY.toPx() }.roundToInt() + itemOffset.y.roundToInt()
                                        )
                                    }
                                    .width(tileWidth)
                                    .height(tileHeight)
                                    .graphicsLayer {
                                        alpha = if (isDragging && extractCandidate) 0f else if (isDragging) 0.9f else 1f
                                        val scale = if (isDragging) 1.08f else 1f
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .onGloballyPositioned { coords ->
                                        if (!isDragging) {
                                            val pos = coords.positionInWindow()
                                            tilePositions[refKey] = pos
                                            val sz = coords.size
                                            appTileBoundsInWindow[refKey] = androidx.compose.ui.geometry.Rect(
                                                pos.x,
                                                pos.y,
                                                pos.x + sz.width,
                                                pos.y + sz.height
                                            )
                                        }
                                    }
                                    .pointerInput(item.id, apps.size, refKey, editMode) {
                                        detectTapOrLongPressDrag(
                                            onTap = { if (!editMode) onLaunch(app) },
                                            onLongPressStart = { touchOffset ->
                                                draggingRefKey = refKey
                                                dragOffset = Offset.Zero
                                                dragTouchOffsetInFolderItem = touchOffset
                                                extractCandidate = false
                                                onDragStateChange(true)
                                                val tilePos = tilePositions[refKey] ?: Offset.Zero
                                                val pointerScreenPos = Offset(
                                                    tilePos.x + touchOffset.x,
                                                    tilePos.y + touchOffset.y
                                                )
                                                onDeleteHoverPositionChange(pointerScreenPos)
                                            },
                                            onDragEnd = {
                                                val moveThresholdPx = with(density) { 10.dp.toPx() }
                                                if (dragOffset.getDistance() <= moveThresholdPx) {
                                                    onShowAppMenu(ref, app)
                                                    resetFolderDrag()
                                                    return@detectTapOrLongPressDrag
                                                }
                                                val tilePos = tilePositions[refKey] ?: Offset.Zero
                                                val tileWidthPx = with(density) { tileWidth.toPx() }
                                                val tileHeightPx = with(density) { tileHeight.toPx() }
                                                val finalScreenPos = tilePos + dragTouchOffsetInFolderItem + dragOffset
                                                val shouldDelete = currentDeleteDropBounds?.contains(finalScreenPos) == true
                                                if (extractCandidate) {
                                                    if (shouldDelete) {
                                                        onDeleteFolderApp(ref)
                                                    } else {
                                                        onExtractAt(ref, finalScreenPos)
                                                    }
                                                } else {
                                                    val targetCol = (baseCol + ((dragOffset.x + dragTouchOffsetInFolderItem.x - tileWidthPx / 2f) / stepXPx).roundToInt()).coerceIn(0, columns - 1)
                                                    val targetRow = (baseRow + ((dragOffset.y + dragTouchOffsetInFolderItem.y - tileHeightPx / 2f) / stepYPx).roundToInt()).coerceIn(0, rows - 1)
                                                    val targetSlot = pageIndex * FOLDER_PAGE_SIZE + targetRow * FOLDER_GRID_COLS + targetCol
                                                    if (targetSlot != assignment.slot) {
                                                        val newRefs = item.appRefs.map { it.copy() }.toMutableList()
                                                        val moving = newRefs.firstOrNull { it.key == refKey } ?: return@detectTapOrLongPressDrag
                                                        val occupant = newRefs.firstOrNull { it.key != refKey && it.slot == targetSlot }
                                                        if (occupant != null) {
                                                            occupant.slot = if (moving.slot >= 0) moving.slot else assignment.slot
                                                        }
                                                        moving.slot = targetSlot
                                                        onReorder(newRefs)
                                                    }
                                                }
                                                resetFolderDrag()
                                            },
                                            onDragCancel = {
                                                resetFolderDrag()
                                            },
                                            onDrag = { _, amount ->
                                                dragOffset += amount
                                                val tilePos = tilePositions[refKey] ?: Offset.Zero
                                                val pointerScreenPos = tilePos + dragTouchOffsetInFolderItem + dragOffset
                                                onDeleteHoverPositionChange(pointerScreenPos)
                                                val bounds = contentBounds
                                                extractCandidate = bounds != null && !bounds.contains(pointerScreenPos)
                                            }
                                        )
                                    }
                            ) {
                                FolderAppTile(app, iconRepository)
                            }
                        }
                    }
                }
            }

            if (pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pageCount) { idx ->
                        val isCurrent = folderPagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (isCurrent) 16.dp else 8.dp, height = 6.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(if (isCurrent) Color.White else Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }

        // Ghost icon following finger when dragging outside folder
        val activeDragKey = draggingRefKey
        if (extractCandidate && activeDragKey != null) {
            val draggedApp = apps.firstOrNull { it.first.key == activeDragKey }?.second
            val tilePos = tilePositions[activeDragKey]
            if (draggedApp != null && tilePos != null) {
                val ghostSizePx = with(density) { 72.dp.toPx() }
                val pointerScreenPos = tilePos + dragTouchOffsetInFolderItem + dragOffset
                val ghostX = pointerScreenPos.x - overlayOriginInWindow.x - ghostSizePx / 2f
                val ghostY = pointerScreenPos.y - overlayOriginInWindow.y - ghostSizePx / 2f
                val ghostIcon = rememberAppIcon(iconRepository, draggedApp)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(ghostX.roundToInt(), ghostY.roundToInt()) }
                        .size(72.dp)
                        .graphicsLayer { alpha = 0.92f }
                ) {
                    IconBadge(ghostIcon, 60.dp)
                }
            }
        }
    }
}

@Composable
private fun FolderAppTile(app: AppCatalogEntry, iconRepository: LauncherIconRepository) {
    val icon = rememberAppIcon(iconRepository, app)
    Box(modifier = Modifier.fillMaxSize()) {
        IconAndLabel(icon = icon, label = app.label, compact = true)
    }
}

private fun formatLabelForTile(raw: String): String {
    val label = raw.trim()
    if (label.isEmpty()) return raw
    if (label.any { it.isWhitespace() }) return label
    val out = StringBuilder(label.length * 2)
    label.forEachIndexed { idx, ch ->
        out.append(ch)
        if (idx != label.lastIndex) out.append('\u200B')
    }
    return out.toString()
}

private fun Char.isCjkGlyph(): Boolean {
    val cp = code
    return (cp in 0x3400..0x4DBF) ||       // CJK Extension A
        (cp in 0x4E00..0x9FFF) ||          // CJK Unified Ideographs
        (cp in 0xF900..0xFAFF) ||          // CJK Compatibility Ideographs
        (cp in 0x2E80..0x2FDF) ||          // CJK Radicals / Kangxi
        (cp in 0x3000..0x303F)             // CJK Symbols and Punctuation
}

@Composable
private fun ItemActionDialog(
    item: HomeItem,
    appIndex: Map<String, AppCatalogEntry>,
    editMode: Boolean,
    canPinToDock: Boolean,
    canSendBackToPage: Boolean,
    canResize: Boolean,
    canUninstallOrInfo: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onPinToDock: () -> Unit,
    onSendBackToPage: () -> Unit,
    onResize: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.labelOrFallback(appIndex)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editMode) {
                    Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Open") }
                    Button(onClick = onRename, modifier = Modifier.fillMaxWidth()) { Text("Rename") }
                    if (canPinToDock) {
                        OutlinedButton(onClick = onPinToDock, modifier = Modifier.fillMaxWidth()) { Text("Pin to dock") }
                    }
                    if (canSendBackToPage) {
                        OutlinedButton(onClick = onSendBackToPage, modifier = Modifier.fillMaxWidth()) { Text("Send to page") }
                    }
                    if (canResize) {
                        OutlinedButton(onClick = onResize, modifier = Modifier.fillMaxWidth()) { Text("Resize widget") }
                    }
                    if (canUninstallOrInfo) {
                        OutlinedButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth()) { Text("App info") }
                        OutlinedButton(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) { Text("Uninstall") }
                    }
                    OutlinedButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) { Text("Remove from home") }
                } else if (canUninstallOrInfo) {
                    OutlinedButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth()) { Text("App info") }
                    OutlinedButton(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) { Text("Uninstall") }
                } else {
                    Text("No actions available", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (editMode) TextButton(onClick = onDismiss) { Text("Close") } else Spacer(Modifier.size(0.dp))
        }
    )
}

@Composable
private fun RenameDialog(value: String, onValueChange: (String) -> Unit, onDismiss: () -> Unit, onSave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(value = value, onValueChange = onValueChange, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun WidgetSizeDialog(
    spanX: Int,
    spanY: Int,
    maxSpanX: Int,
    maxSpanY: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var width by remember(spanX) { mutableIntStateOf(spanX) }
    var height by remember(spanY) { mutableIntStateOf(spanY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resize widget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetAxisEditor("Width", width, maxSpanX, onDecrease = { width = (width - 1).coerceAtLeast(1) }, onIncrease = {
                    width = (width + 1).coerceAtMost(maxSpanX)
                })
                WidgetAxisEditor("Height", height, maxSpanY, onDecrease = { height = (height - 1).coerceAtLeast(1) }, onIncrease = {
                    height = (height + 1).coerceAtMost(maxSpanY)
                })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(width, height) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun WidgetAxisEditor(label: String, value: Int, maxValue: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("$label: $value / $maxValue")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDecrease, enabled = value > 1) { Text("-") }
            OutlinedButton(onClick = onIncrease, enabled = value < maxValue) { Text("+") }
        }
    }
}

private fun commitWidget(
    layout: LayoutConfig,
    appWidgetId: Int,
    placement: EmptyTarget?,
    manager: AppWidgetManager,
    repository: LayoutRepository
): LayoutConfig? {
    val info = manager.getAppWidgetInfo(appWidgetId)
    val target = placement ?: findFirstAvailableAnywhere(layout, 0) ?: return null
    val (spanX, spanY) = estimateWidgetSpan(info, layout.grid)
    val updated = layout.deepCopy()
    val safeTarget = findFirstAvailableOnPage(updated, target.pageIndex, spanX, spanY) ?: return null
    updated.pages[safeTarget.pageIndex].items.add(
        HomeItem(
            type = HomeItemTypes.WIDGET,
            x = safeTarget.x,
            y = safeTarget.y,
            spanX = spanX,
            spanY = spanY,
            title = info?.label,
            appWidgetId = appWidgetId,
            widgetProvider = info?.provider?.flattenToShortString()
        )
    )
    repository.saveLayout(updated)
    return updated
}

private fun findFirstAvailableAnywhere(layout: LayoutConfig, preferredPage: Int, spanX: Int = 1, spanY: Int = 1): EmptyTarget? {
    val pageOrder = (listOf(preferredPage) + layout.pages.indices.filter { it != preferredPage }).distinct()
    pageOrder.forEach { pageIndex ->
        findFirstAvailableOnPage(layout, pageIndex, spanX, spanY)?.let { return it }
    }
    return null
}

private fun findFirstAvailableOnPage(layout: LayoutConfig, pageIndex: Int, spanX: Int = 1, spanY: Int = 1): EmptyTarget? {
    val page = layout.pages.getOrNull(pageIndex) ?: return null
    for (y in 0..layout.grid.rows - spanY) {
        for (x in 0..layout.grid.cols - spanX) {
            if (page.isAreaFree(x, y, spanX, spanY, null)) {
                return EmptyTarget(pageIndex, x, y)
            }
        }
    }
    return null
}

private fun mergePageItemIntoDockFolder(
    layout: LayoutConfig,
    pageIndex: Int,
    itemId: String,
    folderId: String
): LayoutConfig? {
    val updated = layout.deepCopy()
    val page = updated.pages.getOrNull(pageIndex) ?: return null
    val movingItem = page.items.firstOrNull { it.id == itemId } ?: return null
    val folder = updated.dock.firstOrNull { it.id == folderId && it.type == HomeItemTypes.FOLDER } ?: return null
    val refsToAdd: List<AppRef> = when (movingItem.type) {
        HomeItemTypes.APP -> listOf(AppRef(packageName = movingItem.packageName.orEmpty(), activityName = movingItem.activityName.orEmpty()))
        HomeItemTypes.FOLDER -> movingItem.appRefs.map { it.copy() }
        else -> return null
    }
    refsToAdd.forEach { ref ->
        if (folder.appRefs.none { it.key == ref.key }) {
            folder.appRefs.add(ref)
        }
    }
    page.items.removeAll { it.id == itemId }
    return updated
}

private fun moveDockItemToPage(
    layout: LayoutConfig,
    itemId: String,
    targetPage: Int,
    targetX: Int?,
    targetY: Int?
): LayoutConfig? {
    val updated = layout.deepCopy()
    val item = updated.dock.firstOrNull { it.id == itemId } ?: return null
    val destPageIndex = targetPage.coerceIn(0, updated.pages.lastIndex.coerceAtLeast(0))
    val destPage = updated.pages.getOrNull(destPageIndex) ?: return null
    val placement = if (
        targetX != null && targetY != null &&
        targetX in 0 until updated.grid.cols && targetY in 0 until updated.grid.rows &&
        destPage.isAreaFree(targetX, targetY, 1, 1, null)
    ) {
        EmptyTarget(destPageIndex, targetX, targetY)
    } else {
        findFirstAvailableAnywhere(updated, destPageIndex, 1, 1)
    } ?: return null
    updated.dock.removeAll { it.id == itemId }
    item.x = placement.x
    item.y = placement.y
    item.spanX = 1
    item.spanY = 1
    updated.pages[placement.pageIndex].items.add(item)
    return updated
}

private fun bulkMoveItems(
    layout: LayoutConfig,
    pageIndex: Int,
    itemIds: Set<String>,
    leaderId: String,
    targetX: Int,
    targetY: Int,
    pageDelta: Int,
    sendToDock: Boolean,
    hoveredItemId: String?
): LayoutConfig? {
    val updated = layout.deepCopy()
    val sourcePage = updated.pages.getOrNull(pageIndex) ?: return null
    val movingItems = sourcePage.items.filter { it.id in itemIds && it.type == HomeItemTypes.APP && it.spanX == 1 && it.spanY == 1 }
    if (movingItems.isEmpty()) return null

    // Case A: dropped on a folder or app -> merge all into folder (creating one if needed)
    if (hoveredItemId != null && hoveredItemId !in itemIds) {
        val target = sourcePage.items.firstOrNull { it.id == hoveredItemId } ?: return null
        if (target.type == HomeItemTypes.APP && target.spanX == 1 && target.spanY == 1) {
            val seedRefs = mutableListOf(
                AppRef(packageName = target.packageName.orEmpty(), activityName = target.activityName.orEmpty())
            )
            seedRefs.addAll(movingItems.map { AppRef(packageName = it.packageName.orEmpty(), activityName = it.activityName.orEmpty()) })
            target.type = HomeItemTypes.FOLDER
            target.title = target.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Folder"
            target.appRefs = seedRefs.distinctBy { it.key }.toMutableList()
            target.packageName = null
            target.activityName = null
            sourcePage.items.removeAll { it.id in itemIds }
            return updated
        }
        if (target.type == HomeItemTypes.FOLDER) {
            movingItems.forEach { mover ->
                val ref = AppRef(packageName = mover.packageName.orEmpty(), activityName = mover.activityName.orEmpty())
                if (target.appRefs.none { it.key == ref.key }) target.appRefs.add(ref)
            }
            sourcePage.items.removeAll { it.id in itemIds }
            return updated
        }
    }

    // Case B: send all to dock
    if (sendToDock) {
        for (mover in movingItems) {
            val targetSlot = updated.dock.firstEmptyDockSlot() ?: return null
            sourcePage.items.removeAll { it.id == mover.id }
            mover.x = targetSlot
            mover.y = 0
            mover.spanX = 1
            mover.spanY = 1
            updated.dock.add(mover)
        }
        return updated
    }

    // Case C: dropped on empty area / page change -> place leader at (targetX, targetY), others at next available
    val destinationPageIndex = if (pageDelta != 0) (pageIndex + pageDelta).coerceIn(0, updated.pages.lastIndex) else pageIndex
    val destinationPage = updated.pages[destinationPageIndex]
    // Remove all moving items from source first so they don't collide with placement
    sourcePage.items.removeAll { it.id in itemIds }
    // Place leader first
    val leader = movingItems.firstOrNull { it.id == leaderId } ?: movingItems.first()
    val leaderTarget = if (
        targetX in 0 until updated.grid.cols && targetY in 0 until updated.grid.rows &&
        destinationPage.isAreaFree(targetX, targetY, 1, 1, null)
    ) {
        EmptyTarget(destinationPageIndex, targetX, targetY)
    } else {
        findFirstAvailableOnPage(updated, destinationPageIndex, 1, 1)
    } ?: return null
    leader.x = leaderTarget.x
    leader.y = leaderTarget.y
    destinationPage.items.add(leader)
    // Place rest
    for (mover in movingItems) {
        if (mover.id == leader.id) continue
        val slot = findFirstAvailableAnywhere(updated, destinationPageIndex, 1, 1) ?: return null
        mover.x = slot.x
        mover.y = slot.y
        updated.pages[slot.pageIndex].items.add(mover)
    }
    return updated
}

private fun movePageItem(
    layout: LayoutConfig,
    pageIndex: Int,
    itemId: String,
    x: Int,
    y: Int,
    pageDelta: Int = 0,
    sendToDock: Boolean = false,
    didInsert: Boolean = false,
    dockTargetSlot: Int? = null
): LayoutConfig? {
    val updated = layout.deepCopy()
    val page = updated.pages.getOrNull(pageIndex) ?: return null
    val movingItem = page.items.firstOrNull { it.id == itemId } ?: return null
    if (sendToDock && movingItem.type != HomeItemTypes.WIDGET) {
        val preferredSlot = dockTargetSlot?.takeIf { it in 0 until DOCK_SLOTS }
        val targetSlot = preferredSlot ?: updated.dock.firstEmptyDockSlot() ?: return null
        val sourceX = movingItem.x
        val sourceY = movingItem.y
        val occupant = updated.dock.dockItemAt(targetSlot)
        if (occupant != null) {
            val fallbackSlot = updated.dock.firstEmptyDockSlot()
            if (fallbackSlot != null) {
                occupant.x = fallbackSlot
            } else {
                updated.dock.removeAll { it.id == occupant.id }
                val sourcePlacement = if (
                    page.isAreaFree(
                        sourceX,
                        sourceY,
                        occupant.spanX.coerceAtLeast(1),
                        occupant.spanY.coerceAtLeast(1),
                        movingItem.id
                    )
                ) {
                    EmptyTarget(pageIndex, sourceX, sourceY)
                } else {
                    findFirstAvailableAnywhere(
                        updated,
                        pageIndex,
                        occupant.spanX.coerceAtLeast(1),
                        occupant.spanY.coerceAtLeast(1)
                    )
                } ?: return null
                occupant.x = sourcePlacement.x
                occupant.y = sourcePlacement.y
                updated.pages[sourcePlacement.pageIndex].items.add(occupant)
            }
        }
        page.items.removeAll { it.id == itemId }
        movingItem.x = targetSlot
        movingItem.y = 0
        movingItem.spanX = 1
        movingItem.spanY = 1
        updated.dock.add(movingItem)
        return updated
    }
    if (pageDelta != 0) {
        val destinationPage = (pageIndex + pageDelta).coerceIn(0, updated.pages.lastIndex)
        if (destinationPage != pageIndex) {
            val destination = updated.pages[destinationPage]
            val target = if (
                x in 0..updated.grid.cols - movingItem.spanX &&
                y in 0..updated.grid.rows - movingItem.spanY &&
                destination.isAreaFree(x, y, movingItem.spanX, movingItem.spanY, null)
            ) {
                EmptyTarget(destinationPage, x, y)
            } else {
                findFirstAvailableOnPage(updated, destinationPage, movingItem.spanX, movingItem.spanY)
            } ?: return null
            page.items.removeAll { it.id == itemId }
            movingItem.x = target.x
            movingItem.y = target.y
            updated.pages[destinationPage].items.add(movingItem)
            return updated
        }
    }
    if (didInsert) {
        if (insertItemWithPush(page, movingItem, x, y, updated.grid)) {
            return updated
        }
        return null
    }
    if (page.isAreaFree(x, y, movingItem.spanX, movingItem.spanY, movingItem.id)) {
        movingItem.x = x
        movingItem.y = y
        return updated
    }

    val target = page.items.firstOrNull { candidate ->
        candidate.id != itemId &&
            x < candidate.x + candidate.spanX &&
            x + movingItem.spanX > candidate.x &&
            y < candidate.y + candidate.spanY &&
            y + movingItem.spanY > candidate.y
    }
    if (target != null) {
        if (movingItem.type == HomeItemTypes.APP && movingItem.spanX == 1 && movingItem.spanY == 1) {
            if (target.type == HomeItemTypes.APP && target.spanX == 1 && target.spanY == 1) {
                page.items.removeAll { it.id == itemId }
                target.type = HomeItemTypes.FOLDER
                target.title = target.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Folder"
                target.appRefs = mutableListOf(
                    AppRef(packageName = target.packageName.orEmpty(), activityName = target.activityName.orEmpty()),
                    AppRef(packageName = movingItem.packageName.orEmpty(), activityName = movingItem.activityName.orEmpty())
                ).distinctBy { it.key }.toMutableList()
                target.packageName = null
                target.activityName = null
                return updated
            }
            if (target.type == HomeItemTypes.FOLDER) {
                val movingRef = AppRef(packageName = movingItem.packageName.orEmpty(), activityName = movingItem.activityName.orEmpty())
                if (target.appRefs.none { it.key == movingRef.key }) {
                    page.items.removeAll { it.id == itemId }
                    if (target.title.isNullOrBlank()) {
                        target.title = "Folder"
                    }
                    target.appRefs.add(movingRef)
                    return updated
                }
            }
        }
        val oldX = movingItem.x
        val oldY = movingItem.y
        movingItem.x = x
        movingItem.y = y
        target.x = oldX
        target.y = oldY
        return updated
    }

    return null
}

private fun insertItemWithPush(page: PageLayout, movingItem: HomeItem, x: Int, y: Int, grid: GridSpec): Boolean {
    if (movingItem.spanX != 1 || movingItem.spanY != 1) return false
    if (x !in 0 until grid.cols || y !in 0 until grid.rows) return false

    val rowItems = page.items
        .filter { it.id != movingItem.id && it.y == y && it.spanY == 1 }
        .associateBy { it.x }

    if (rowItems[x] == null) {
        movingItem.x = x
        movingItem.y = y
        return true
    }

    val tryDirection = { dir: Int ->
        var freeX = -1
        var step = x
        while (step in 0 until grid.cols) {
            val candidate = rowItems[step]
            if (candidate == null) {
                freeX = step
                break
            }
            if (candidate.spanX != 1 || candidate.spanY != 1 || candidate.type == HomeItemTypes.WIDGET) {
                break
            }
            step += dir
        }
        if (freeX == -1) {
            false
        } else {
            if (dir > 0) {
                var cur = freeX - 1
                while (cur >= x) {
                    rowItems[cur]?.x = cur + 1
                    cur -= 1
                }
            } else {
                var cur = freeX + 1
                while (cur <= x) {
                    rowItems[cur]?.x = cur - 1
                    cur += 1
                }
            }
            movingItem.x = x
            movingItem.y = y
            true
        }
    }

    return tryDirection(1) || tryDirection(-1)
}

private data class PushPreview(val ids: Set<String>, val direction: Int)

private fun computePushPreview(page: PageLayout, draggingId: String, x: Int, y: Int, cols: Int): PushPreview {
    if (x !in 0 until cols) return PushPreview(emptySet(), 1)
    val rowItems = page.items
        .filter { it.id != draggingId && it.y == y && it.spanY == 1 }
        .associateBy { it.x }
    if (rowItems[x] == null) return PushPreview(emptySet(), 1)

    val collectChain = { dir: Int ->
        val collected = mutableListOf<String>()
        var step = x
        var hasFree = false
        while (step in 0 until cols) {
            val candidate = rowItems[step]
            if (candidate == null) { hasFree = true; break }
            if (candidate.spanX != 1 || candidate.spanY != 1 || candidate.type == HomeItemTypes.WIDGET) {
                break
            }
            collected += candidate.id
            step += dir
        }
        if (hasFree) collected else emptyList()
    }

    val rightChain = collectChain(1)
    if (rightChain.isNotEmpty()) return PushPreview(rightChain.toSet(), 1)
    val leftChain = collectChain(-1)
    if (leftChain.isNotEmpty()) return PushPreview(leftChain.toSet(), -1)
    return PushPreview(emptySet(), 1)
}

private fun removePage(layout: LayoutConfig, pageIndex: Int): LayoutConfig? {
    if (layout.pages.size <= 1) return null
    val updated = layout.deepCopy()
    val page = updated.pages.getOrNull(pageIndex) ?: return null
    val itemsToMove = page.items.map { it.deepCopy() }
    updated.pages.removeAt(pageIndex)
    updated.pages.forEachIndexed { index, candidate -> candidate.index = index }
    itemsToMove.forEach { item ->
        val target = findFirstAvailableAnywhere(updated, pageIndex.coerceAtMost(updated.pages.lastIndex), item.spanX, item.spanY) ?: return null
        item.x = target.x
        item.y = target.y
        updated.pages[target.pageIndex].items.add(item)
    }
    return updated
}

private fun reorderDock(layout: LayoutConfig, itemId: String, targetIndex: Int): LayoutConfig? {
    val updated = layout.deepCopy()
    val item = updated.dock.firstOrNull { it.id == itemId } ?: return null
    val safeTarget = targetIndex.coerceIn(0, DOCK_SLOTS - 1)
    if (item.x == safeTarget) return updated
    val previousSlot = item.x.coerceIn(0, DOCK_SLOTS - 1)
    val occupant = updated.dock.firstOrNull { it.id != itemId && it.x == safeTarget }
    item.x = safeTarget
    occupant?.x = previousSlot
    return updated
}

private fun locateItem(layout: LayoutConfig, itemId: String): ItemTarget? {
    layout.pages.forEach { page ->
        if (page.items.any { it.id == itemId }) {
            return ItemTarget(Area.PAGE, pageIndex = page.index, itemId = itemId)
        }
    }
    if (layout.dock.any { it.id == itemId }) {
        return ItemTarget(Area.DOCK, itemId = itemId)
    }
    return null
}

private fun LayoutConfig.findItem(target: ItemTarget): HomeItem? {
    return when (target.area) {
        Area.DOCK -> dock.firstOrNull { it.id == target.itemId }?.copy(x = -1)
        Area.PAGE -> pages.getOrNull(target.pageIndex ?: return null)?.items?.firstOrNull { it.id == target.itemId }
    }
}

private fun LayoutConfig.removeItem(target: ItemTarget): HomeItem? {
    val removed = when (target.area) {
        Area.DOCK -> {
            val item = dock.firstOrNull { it.id == target.itemId }
            dock.removeAll { it.id == target.itemId }
            item
        }
        Area.PAGE -> {
            val page = pages.getOrNull(target.pageIndex ?: return null)
            val item = page?.items?.firstOrNull { it.id == target.itemId }
            page?.items?.removeAll { it.id == target.itemId }
            item
        }
    }
    return removed
}

private fun LayoutConfig.removePageItems(pageIndex: Int, itemIds: Set<String>): List<HomeItem> {
    val page = pages.getOrNull(pageIndex) ?: return emptyList()
    val removed = page.items.filter { it.id in itemIds }
    if (removed.isNotEmpty()) {
        page.items.removeAll { it.id in itemIds }
    }
    return removed
}

private fun LayoutConfig.renameItem(target: ItemTarget, title: String) {
    when (target.area) {
        Area.DOCK -> dock.firstOrNull { it.id == target.itemId }
        Area.PAGE -> pages.getOrNull(target.pageIndex ?: return)?.items?.firstOrNull { it.id == target.itemId }
    }?.title = title.trim().ifBlank { null }
}

private fun LayoutConfig.updateFolderApps(folderId: String, reordered: MutableList<AppRef>) {
    val safeCopy = reordered.map { it.copy() }.toMutableList()
    pages.forEach { page ->
        page.items.firstOrNull { it.id == folderId }?.let {
            it.appRefs = safeCopy
            return
        }
    }
    dock.firstOrNull { it.id == folderId }?.let { it.appRefs = safeCopy }
}

private fun LayoutConfig.findFolderById(folderId: String): HomeItem? {
    pages.forEach { page ->
        page.items.firstOrNull { it.id == folderId }?.let { return it }
    }
    return dock.firstOrNull { it.id == folderId }
}

private fun LayoutConfig.removeAppFromFolder(folderId: String, ref: AppRef): Boolean {
    val folder = findFolderById(folderId) ?: return false
    if (folder.appRefs.none { it.key == ref.key }) return false

    folder.appRefs.removeAll { it.key == ref.key }
    if (folder.appRefs.size == 1) {
        val remaining = folder.appRefs.first()
        folder.type = HomeItemTypes.APP
        folder.packageName = remaining.packageName
        folder.activityName = remaining.activityName
        folder.title = null
        folder.appRefs = mutableListOf()
    } else if (folder.appRefs.isEmpty()) {
        pages.forEach { it.items.removeAll { item -> item.id == folderId } }
        dock.removeAll { it.id == folderId }
    }
    return true
}

private fun LayoutConfig.extractAppFromFolder(folderId: String, ref: AppRef, preferredPage: Int): Boolean {
    return extractAppFromFolderToTarget(folderId, ref, preferredPage, null, null)
}

private fun LayoutConfig.extractAppFromFolderToTarget(
    folderId: String,
    ref: AppRef,
    preferredPage: Int,
    targetX: Int?,
    targetY: Int?
): Boolean {
    val folder = findFolderById(folderId) ?: return false
    if (folder.appRefs.none { it.key == ref.key }) return false

    val targetPageIndex = preferredPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    val placement = if (
        targetX != null && targetY != null &&
        targetX in 0 until grid.cols && targetY in 0 until grid.rows &&
        pages.getOrNull(targetPageIndex)?.isAreaFree(targetX, targetY, 1, 1, null) == true
    ) {
        EmptyTarget(targetPageIndex, targetX, targetY)
    } else {
        findFirstAvailableAnywhere(this, targetPageIndex, 1, 1)
    } ?: return false

    folder.appRefs.removeAll { it.key == ref.key }
    pages[placement.pageIndex].items.add(
        HomeItem(
            type = HomeItemTypes.APP,
            x = placement.x,
            y = placement.y,
            packageName = ref.packageName,
            activityName = ref.activityName
        )
    )
    if (folder.appRefs.size == 1) {
        val remaining = folder.appRefs.first()
        folder.type = HomeItemTypes.APP
        folder.packageName = remaining.packageName
        folder.activityName = remaining.activityName
        folder.title = null
        folder.appRefs = mutableListOf()
    } else if (folder.appRefs.isEmpty()) {
        pages.forEach { it.items.removeAll { item -> item.id == folderId } }
        dock.removeAll { it.id == folderId }
    }
    return true
}

private fun LayoutConfig.moveItemToDock(target: ItemTarget) {
    if (target.area != Area.PAGE) return
    val targetSlot = dock.firstEmptyDockSlot() ?: return
    val page = pages.getOrNull(target.pageIndex ?: return) ?: return
    val item = page.items.firstOrNull { it.id == target.itemId } ?: return
    page.items.removeAll { it.id == target.itemId }
    item.x = targetSlot
    item.y = 0
    item.spanX = 1
    item.spanY = 1
    dock.add(item)
}

private fun LayoutConfig.moveItemFromDockToPage(target: ItemTarget, preferredPage: Int) {
    val item = dock.firstOrNull { it.id == target.itemId } ?: return
    val destination = findFirstAvailableAnywhere(this, preferredPage, item.spanX, item.spanY) ?: return
    dock.removeAll { it.id == target.itemId }
    item.x = destination.x
    item.y = destination.y
    pages[destination.pageIndex].items.add(item)
}

private fun LayoutConfig.resizeWidget(target: ItemTarget) {
    if (target.area != Area.PAGE) return
    val pageIndex = target.pageIndex ?: return
    val item = pages.getOrNull(pageIndex)?.items?.firstOrNull { it.id == target.itemId } ?: return
    if (item.type != HomeItemTypes.WIDGET) return
    val newSpanX = if (item.spanX < grid.cols) item.spanX + 1 else 2
    val newSpanY = if (item.spanY < grid.rows) item.spanY + 1 else 1
    if (pages[pageIndex].isAreaFree(item.x, item.y, newSpanX.coerceAtMost(grid.cols), newSpanY.coerceAtMost(grid.rows), item.id)) {
        item.spanX = newSpanX.coerceAtMost(grid.cols)
        item.spanY = newSpanY.coerceAtMost(grid.rows)
    }
}

private fun LayoutConfig.adjustWidgetSize(target: ItemTarget, spanX: Int, spanY: Int) {
    if (target.area != Area.PAGE) return
    val pageIndex = target.pageIndex ?: return
    val item = pages.getOrNull(pageIndex)?.items?.firstOrNull { it.id == target.itemId } ?: return
    if (item.type != HomeItemTypes.WIDGET) return
    val safeX = spanX.coerceIn(1, grid.cols)
    val safeY = spanY.coerceIn(1, grid.rows)
    if (pages[pageIndex].isAreaFree(item.x, item.y, safeX, safeY, item.id)) {
        item.spanX = safeX
        item.spanY = safeY
    }
}

private fun List<HomeItem>.dockItemAt(slot: Int): HomeItem? {
    return firstOrNull { it.x == slot }
}

private fun List<HomeItem>.firstEmptyDockSlot(): Int? {
    for (slot in 0 until DOCK_SLOTS) {
        if (none { it.x == slot }) return slot
    }
    return null
}

private fun PageLayout.isAreaFree(x: Int, y: Int, spanX: Int, spanY: Int, ignoredId: String?): Boolean {
    return items.none { item ->
        if (item.id == ignoredId) {
            false
        } else {
            val intersectsX = x < item.x + item.spanX && x + spanX > item.x
            val intersectsY = y < item.y + item.spanY && y + spanY > item.y
            intersectsX && intersectsY
        }
    }
}
