package com.openai.launcherstudio

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

data class PendingShortcut(
    val intentUri: String,
    val label: String,
    val iconPath: String?,
    val packageName: String?,
    val shortcutId: String? = null,
    val persisted: Boolean = false
)

object ShortcutInbox {
    @Volatile
    private var listener: ((PendingShortcut) -> Unit)? = null
    private val pending = mutableListOf<PendingShortcut>()
    private val lock = Any()

    fun setListener(callback: ((PendingShortcut) -> Unit)?) {
        synchronized(lock) {
            listener = callback
            if (callback != null && pending.isNotEmpty()) {
                val drained = pending.toList()
                pending.clear()
                drained.forEach(callback)
            }
        }
    }

    fun publish(shortcut: PendingShortcut) {
        synchronized(lock) {
            val cb = listener
            if (cb != null) cb(shortcut) else pending.add(shortcut)
        }
    }
}

object AppInstallInbox {
    @Volatile
    private var listener: (() -> Unit)? = null
    private var pending = false
    private val lock = Any()

    fun setListener(callback: (() -> Unit)?) {
        synchronized(lock) {
            listener = callback
            if (callback != null && pending) {
                pending = false
                callback()
            }
        }
    }

    fun publish() {
        synchronized(lock) {
            listener?.invoke() ?: run { pending = true }
        }
    }
}

class PackageAddedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
        val packageName = intent.data?.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: return
        val added = runCatching {
            LayoutRepository(context).addInstalledPackage(packageName) != null
        }.getOrDefault(false)
        if (added) AppInstallInbox.publish()
    }
}

class InstallShortcutReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.android.launcher.action.INSTALL_SHORTCUT") return
        val name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Shortcut"
        val shortcutIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT) ?: return
        val bitmap = intent.getParcelableExtra<Bitmap>(Intent.EXTRA_SHORTCUT_ICON)
        val iconPath = bitmap?.let { saveShortcutIcon(context, it) }
        val pkg = shortcutIntent.`package` ?: shortcutIntent.component?.packageName
        val shortcut = PendingShortcut(
            intentUri = shortcutIntent.toUri(0),
            label = name,
            iconPath = iconPath,
            packageName = pkg
        )
        val saved = runCatching { LayoutRepository(context).addShortcut(shortcut) != null }.getOrDefault(false)
        ShortcutInbox.publish(shortcut.copy(persisted = saved))
        Toast.makeText(context, if (saved) "Added \"$name\"" else "No room for \"$name\"", Toast.LENGTH_SHORT).show()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun handlePinShortcutIntent(context: Context, intent: Intent): Boolean {
    if (intent.action != "android.content.pm.action.CONFIRM_PIN_SHORTCUT") return false
    val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return false
    val request = launcherApps.getPinItemRequest(intent) ?: return false
    if (request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
        request.accept()
        return true
    }
    val info: ShortcutInfo = request.shortcutInfo ?: run {
        request.accept()
        return true
    }
    val label = info.shortLabel?.toString() ?: info.longLabel?.toString() ?: "Shortcut"
    val launchIntent = runCatching { info.intent }.getOrNull()
        ?: Intent(Intent.ACTION_MAIN)
            .setPackage(info.`package`)
            .putExtra("shortcut_id", info.id)
    val drawable: Drawable? = runCatching {
        launcherApps.getShortcutIconDrawable(info, context.resources.displayMetrics.densityDpi)
    }.getOrNull()
    val iconPath = drawable?.toBitmapSafe()?.let { saveShortcutIcon(context, it) }
    if (!request.accept()) return true
    val shortcut = PendingShortcut(
        intentUri = launchIntent.toUri(0),
        label = label,
        iconPath = iconPath,
        packageName = info.`package`,
        shortcutId = info.id
    )
    val saved = runCatching { LayoutRepository(context).addShortcut(shortcut) != null }.getOrDefault(false)
    ShortcutInbox.publish(shortcut.copy(persisted = saved))
    Toast.makeText(context, if (saved) "Added \"$label\"" else "No room for \"$label\"", Toast.LENGTH_SHORT).show()
    return true
}

internal fun saveShortcutIcon(context: Context, bitmap: Bitmap): String? {
    return runCatching {
        val dir = File(context.filesDir, "shortcut-icons").apply { mkdirs() }
        val file = File(dir, "icon-${Instant.now().toEpochMilli()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }.getOrNull()
}

internal fun Drawable.toBitmapSafe(): Bitmap? {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val w = intrinsicWidth.takeIf { it > 0 } ?: 144
    val h = intrinsicHeight.takeIf { it > 0 } ?: 144
    return runCatching {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bmp
    }.getOrNull()
}

fun launchShortcut(context: Context, item: HomeItem) {
    val uri = item.shortcutIntentUri ?: return
    val intent = runCatching { Intent.parseUri(uri, 0) }.getOrNull() ?: return
    val shortcutId = item.shortcutId
    val pkg = item.packageName ?: intent.`package` ?: intent.component?.packageName
    if (shortcutId != null && pkg != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val handle = Process.myUserHandle()
        runCatching {
            launcherApps?.startShortcut(pkg, shortcutId, null, null, handle)
            return
        }
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
