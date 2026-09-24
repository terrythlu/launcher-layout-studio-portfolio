package com.openai.launcherstudio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.VelocityTracker
import android.view.WindowManager
import android.widget.ImageView
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

class FloatingButtonService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hideRingRunnable: Runnable? = null
    private var ringVisible = true
    private var snapAnimator: ValueAnimator? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        applyAppearance()
    }

    private val foregroundListener: () -> Unit = {
        updateVisibility()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!canShowOverlay(this)) {
            stopSelf()
            return
        }
        showFloatingButton()
        prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
        FloatingButtonStateBus.addListener(foregroundListener)
        updateVisibility()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canShowOverlay(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener) }
        FloatingButtonStateBus.removeListener(foregroundListener)
        hideRingRunnable?.let { mainHandler.removeCallbacks(it) }
        hideRingRunnable = null
        snapAnimator?.cancel()
        snapAnimator = null
        floatingView?.let { runCatching { windowManager?.removeView(it) } }
        floatingView = null
        windowManager = null
    }

    private fun showFloatingButton() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val view = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher_round)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = getString(R.string.app_name)
            elevation = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics
            )
        }
        floatingView = view

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val initialOuter = dp(getOuterSize(this))
        val lp = WindowManager.LayoutParams(
            initialOuter, initialOuter,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - initialOuter - dp(16)
            y = resources.displayMetrics.heightPixels / 2
        }
        params = lp
        applyAppearance()

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initX = 0
            private var initY = 0
            private var touchStartX = 0f
            private var touchStartY = 0f
            private var moved = false
            private val slop = dp(8)
            private var velocityTracker: VelocityTracker? = null

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        snapAnimator?.cancel()
                        velocityTracker?.recycle()
                        velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
                        initX = lp.x
                        initY = lp.y
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                        moved = false
                        showRingNow()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        velocityTracker?.addMovement(event)
                        val dx = event.rawX - touchStartX
                        val dy = event.rawY - touchStartY
                        if (abs(dx) > slop || abs(dy) > slop) moved = true
                        lp.x = (initX + dx).toInt()
                        lp.y = (initY + dy).toInt()
                        runCatching { wm.updateViewLayout(view, lp) }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        velocityTracker?.addMovement(event)
                        velocityTracker?.computeCurrentVelocity(1000)
                        val velocityX = velocityTracker?.xVelocity ?: 0f
                        val velocityY = velocityTracker?.yVelocity ?: 0f
                        velocityTracker?.recycle()
                        velocityTracker = null

                        if (!moved) {
                            val launchIntent = Intent(this@FloatingButtonService, MainActivity::class.java)
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                )
                            runCatching { startActivity(launchIntent) }
                        } else {
                            snapToEdge(velocityX, velocityY)
                        }
                        scheduleRingHide()
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        velocityTracker?.recycle()
                        velocityTracker = null
                        snapToEdge()
                        scheduleRingHide()
                        return true
                    }
                }
                return false
            }
        })

        runCatching { wm.addView(view, lp) }
        showRingNow()
        scheduleRingHide()
    }

    private fun applyAppearance() {
        val view = floatingView ?: return
        val lp = params ?: return
        val outerDp = getOuterSize(this)
        val iconDp = getIconSize(this).coerceAtMost(outerDp)
        val alpha = getAlpha(this)
        val outerPx = dp(outerDp)
        val iconPx = dp(iconDp)
        val padPx = ((outerPx - iconPx) / 2).coerceAtLeast(0)

        if (lp.width != outerPx || lp.height != outerPx) {
            lp.width = outerPx
            lp.height = outerPx
            runCatching { windowManager?.updateViewLayout(view, lp) }
        }
        view.setPadding(padPx, padPx, padPx, padPx)
        view.alpha = 1f
        if (ringVisible) {
            val ringAlphaInt = (alpha * 255).toInt().coerceIn(0, 255)
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(AndroidColor.argb(ringAlphaInt, 255, 255, 255))
                setStroke(dp(1), AndroidColor.parseColor("#33000000"))
            }
        } else {
            view.background = null
        }
    }

    private fun updateVisibility() {
        val view = floatingView ?: return
        // Hide while in our app, EXCEPT when the settings sheet is open
        // (so the user can see live effect of the sliders).
        val shouldHide = FloatingButtonStateBus.isAppForeground &&
            !FloatingButtonStateBus.isSettingsVisible
        val wasVisible = view.visibility == View.VISIBLE
        view.visibility = if (shouldHide) View.GONE else View.VISIBLE
        if (shouldHide) {
            hideRingRunnable?.let { mainHandler.removeCallbacks(it) }
        } else if (!wasVisible) {
            showRingNow()
            scheduleRingHide()
        }
    }

    private fun showRingNow() {
        if (!ringVisible) {
            ringVisible = true
            applyAppearance()
        }
    }

    private fun scheduleRingHide() {
        hideRingRunnable?.let { mainHandler.removeCallbacks(it) }
        hideRingRunnable = Runnable {
            if (ringVisible) {
                ringVisible = false
                applyAppearance()
            }
        }.also { mainHandler.postDelayed(it, RING_HIDE_DELAY_MS) }
    }

    private fun snapToEdge(velocityX: Float = 0f, velocityY: Float = 0f) {
        val lp = params ?: return
        val wm = windowManager ?: return
        val view = floatingView ?: return
        val screenW = resources.displayMetrics.widthPixels
        val margin = dp(8)
        val minX = margin
        val maxX = screenW - lp.width - margin
        if (maxX <= minX) return

        val flingThreshold = getFlingTriggerVelocity(this).toFloat()
        val horizontalEnough = abs(velocityX) >= abs(velocityY) * FLING_HORIZONTAL_DOMINANCE_RATIO ||
            abs(velocityX) >= flingThreshold * FLING_STRONG_OVERRIDE_MULTIPLIER
        val useFling = abs(velocityX) >= flingThreshold && horizontalEnough
        val targetX = when {
            useFling && velocityX > 0f -> maxX
            useFling && velocityX < 0f -> minX
            lp.x + lp.width / 2 < screenW / 2 -> minX
            else -> maxX
        }

        animateSnapX(wm, view, lp, targetX, if (useFling) velocityX else 0f)
    }

    private fun animateSnapX(
        wm: WindowManager,
        view: View,
        lp: WindowManager.LayoutParams,
        targetX: Int,
        velocityX: Float
    ) {
        val startX = lp.x
        if (startX == targetX) {
            runCatching { wm.updateViewLayout(view, lp) }
            return
        }
        val distance = abs(targetX - startX).toFloat()
        val durationMs = if (abs(velocityX) > 0f) {
            ((distance / abs(velocityX)) * 1000f * 1.3f).toLong().coerceIn(120L, 360L)
        } else {
            220L
        }

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator(if (abs(velocityX) > 0f) 1.1f else 1.8f)
            addUpdateListener { animator ->
                lp.x = animator.animatedValue as Int
                runCatching { wm.updateViewLayout(view, lp) }
            }
            start()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PREFS_NAME = "floating_button_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ALPHA = "alpha"
        private const val KEY_OUTER_SIZE_DP = "outer_size_dp"
        private const val KEY_ICON_SIZE_DP = "icon_size_dp"
        private const val KEY_FLING_TRIGGER_PX_PER_SEC = "fling_trigger_px_per_sec"

        const val DEFAULT_ALPHA = 0.88f
        const val DEFAULT_OUTER_SIZE_DP = 56
        const val DEFAULT_ICON_SIZE_DP = 40
        const val DEFAULT_FLING_TRIGGER_PX_PER_SEC = 500
        const val MIN_ALPHA = 0.0f
        const val MAX_ALPHA = 1.0f
        const val MIN_OUTER_DP = 40
        const val MAX_OUTER_DP = 96
        const val MIN_ICON_DP = 4
        const val MAX_ICON_DP = 88
        const val MIN_FLING_TRIGGER_PX_PER_SEC = 80
        const val MAX_FLING_TRIGGER_PX_PER_SEC = 2000
        const val FLING_TRIGGER_STEP_PX_PER_SEC = 20
        private const val RING_HIDE_DELAY_MS = 5000L
        private const val FLING_HORIZONTAL_DOMINANCE_RATIO = 0.2f
        private const val FLING_STRONG_OVERRIDE_MULTIPLIER = 1.35f

        fun isEnabled(context: Context): Boolean =
            prefs(context).getBoolean(KEY_ENABLED, false)

        fun setEnabled(context: Context, enabled: Boolean) {
            prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
            if (enabled) start(context) else stop(context)
        }

        fun getAlpha(context: Context): Float =
            prefs(context).getFloat(KEY_ALPHA, DEFAULT_ALPHA).coerceIn(MIN_ALPHA, MAX_ALPHA)

        fun setAlpha(context: Context, alpha: Float) {
            prefs(context).edit().putFloat(KEY_ALPHA, alpha.coerceIn(MIN_ALPHA, MAX_ALPHA)).apply()
        }

        fun getOuterSize(context: Context): Int =
            prefs(context).getInt(KEY_OUTER_SIZE_DP, DEFAULT_OUTER_SIZE_DP).coerceIn(MIN_OUTER_DP, MAX_OUTER_DP)

        fun setOuterSize(context: Context, dp: Int) {
            prefs(context).edit().putInt(KEY_OUTER_SIZE_DP, dp.coerceIn(MIN_OUTER_DP, MAX_OUTER_DP)).apply()
        }

        fun getIconSize(context: Context): Int =
            prefs(context).getInt(KEY_ICON_SIZE_DP, DEFAULT_ICON_SIZE_DP).coerceIn(MIN_ICON_DP, MAX_ICON_DP)

        fun setIconSize(context: Context, dp: Int) {
            prefs(context).edit().putInt(KEY_ICON_SIZE_DP, dp.coerceIn(MIN_ICON_DP, MAX_ICON_DP)).apply()
        }

        fun getFlingTriggerVelocity(context: Context): Int =
            prefs(context).getInt(KEY_FLING_TRIGGER_PX_PER_SEC, DEFAULT_FLING_TRIGGER_PX_PER_SEC)
                .coerceIn(MIN_FLING_TRIGGER_PX_PER_SEC, MAX_FLING_TRIGGER_PX_PER_SEC)

        fun setFlingTriggerVelocity(context: Context, velocityPxPerSec: Int) {
            prefs(context).edit()
                .putInt(
                    KEY_FLING_TRIGGER_PX_PER_SEC,
                    velocityPxPerSec.coerceIn(MIN_FLING_TRIGGER_PX_PER_SEC, MAX_FLING_TRIGGER_PX_PER_SEC)
                )
                .apply()
        }

        fun start(context: Context) {
            runCatching {
                context.startService(Intent(context, FloatingButtonService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, FloatingButtonService::class.java))
            }
        }

        fun canShowOverlay(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

object FloatingButtonStateBus {
    @Volatile var isAppForeground: Boolean = false
        private set
    @Volatile var isSettingsVisible: Boolean = false
        private set
    private val listeners = mutableListOf<() -> Unit>()

    fun setForeground(value: Boolean) {
        if (isAppForeground == value) return
        isAppForeground = value
        notifyListeners()
    }

    fun setSettingsVisible(value: Boolean) {
        if (isSettingsVisible == value) return
        isSettingsVisible = value
        notifyListeners()
    }

    private fun notifyListeners() {
        synchronized(listeners) {
            listeners.toList()
        }.forEach { runCatching { it() } }
    }

    fun addListener(listener: () -> Unit) {
        synchronized(listeners) { listeners += listener }
    }

    fun removeListener(listener: () -> Unit) {
        synchronized(listeners) { listeners -= listener }
    }
}
