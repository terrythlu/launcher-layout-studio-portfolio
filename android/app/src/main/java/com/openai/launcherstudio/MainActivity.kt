package com.openai.launcherstudio

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.content.IntentFilter
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

private const val APP_WIDGET_HOST_ID = 2048

class MainActivity : ComponentActivity() {
    val appWidgetHost: AppWidgetHost by lazy { AppWidgetHost(this, APP_WIDGET_HOST_ID) }
    var homeGestureListener: ((deltaX: Float, deltaY: Float, maxHorizontal: Float, isFinal: Boolean) -> Unit)? = null
    var homeGestureIgnoreRegion: RectF? = null
    private val packageAddedReceiver = PackageAddedReceiver()
    private var gestureDownX = 0f
    private var gestureDownY = 0f
    private var gestureMaxHorizontal = 0f
    private var gesturePeakDeltaY = 0f
    private var gestureActive = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        var completedGesture: Triple<Float, Float, Float>? = null
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureDownX = event.x
                gestureDownY = event.y
                gestureMaxHorizontal = 0f
                gesturePeakDeltaY = 0f
                gestureActive = homeGestureIgnoreRegion?.contains(event.x, event.y) != true
            }
            MotionEvent.ACTION_MOVE -> if (gestureActive) {
                gestureMaxHorizontal = maxOf(gestureMaxHorizontal, kotlin.math.abs(event.x - gestureDownX))
                val deltaY = event.y - gestureDownY
                if (kotlin.math.abs(deltaY) > kotlin.math.abs(gesturePeakDeltaY)) {
                    gesturePeakDeltaY = deltaY
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (gestureActive) {
                val deltaX = event.x - gestureDownX
                val finalDeltaY = event.y - gestureDownY
                val deltaY = if (kotlin.math.abs(gesturePeakDeltaY) > kotlin.math.abs(finalDeltaY)) {
                    gesturePeakDeltaY
                } else {
                    finalDeltaY
                }
                gestureMaxHorizontal = maxOf(gestureMaxHorizontal, kotlin.math.abs(deltaX))
                completedGesture = Triple(deltaX, deltaY, gestureMaxHorizontal)
                gestureActive = false
            }
        }
        val handled = super.dispatchTouchEvent(event)
        completedGesture?.let { (deltaX, deltaY, maxHorizontal) ->
            homeGestureListener?.invoke(deltaX, deltaY, maxHorizontal, true)
        }
        return handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_LauncherLayoutStudio)
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        val packageFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply { addDataScheme("package") }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageAddedReceiver, packageFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(packageAddedReceiver, packageFilter)
        }

        if (
            FloatingButtonService.isEnabled(this) &&
            FloatingButtonService.canShowOverlay(this)
        ) {
            FloatingButtonService.start(this)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    LauncherRoot(activity = this)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            intent.action == "android.content.pm.action.CONFIRM_PIN_SHORTCUT"
        ) {
            handlePinShortcutIntent(this, intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // Defer widget host binding so first frame can appear immediately.
        window.decorView.post {
            runCatching { appWidgetHost.startListening() }
        }
    }

    override fun onResume() {
        super.onResume()
        FloatingButtonStateBus.setForeground(true)
    }

    override fun onPause() {
        FloatingButtonStateBus.setForeground(false)
        super.onPause()
    }

    override fun onStop() {
        runCatching { appWidgetHost.stopListening() }
        super.onStop()
    }

    override fun onDestroy() {
        homeGestureListener = null
        homeGestureIgnoreRegion = null
        runCatching { unregisterReceiver(packageAddedReceiver) }
        super.onDestroy()
    }
}
