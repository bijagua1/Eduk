package com.eduk.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.eduk.app.ui.MainActivity

/**
 * Full-screen protective shield displayed while a parent-authorized learning gate
 * is pending. It consumes touches above the restricted app and returns the child
 * to the verified Eduk challenge instead of leaving the gate evadable via Recents.
 */
class GateOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        private const val ACTION_SHOW = "com.eduk.app.action.SHOW_GATE"
        private const val ACTION_HIDE = "com.eduk.app.action.HIDE_GATE"

        fun canDrawOverlays(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        fun show(context: Context) {
            if (!canDrawOverlays(context)) return
            runCatching {
                context.startService(Intent(context, GateOverlayService::class.java).setAction(ACTION_SHOW))
            }
        }

        fun hide(context: Context) {
            runCatching {
                context.startService(Intent(context, GateOverlayService::class.java).setAction(ACTION_HIDE))
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) removeOverlay() else addOverlay()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addOverlay() {
        if (overlayView != null || !canDrawOverlays(this)) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = FrameLayout(this).apply {
            setBackgroundColor(AndroidColor.parseColor("#ED0B1F3A"))
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, _ -> true }
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(30), dp(34), dp(30), dp(30))
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(AndroidColor.WHITE)
            }
            layoutParams = FrameLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.84f).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }
        val shield = TextView(this).apply {
            text = "EDUK"
            setTextColor(AndroidColor.parseColor("#FF7A1A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        val title = TextView(this).apply {
            text = "Time to answer a question"
            setTextColor(AndroidColor.parseColor("#0B1F3A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        val subtitle = TextView(this).apply {
            text = "Your parent set a learning check before this app can continue."
            setTextColor(AndroidColor.parseColor("#52677F"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        }
        val button = Button(this).apply {
            text = "Answer now"
            setTextColor(AndroidColor.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(AndroidColor.parseColor("#FF7A1A"))
            }
            setPadding(dp(26), dp(12), dp(26), dp(12))
            setOnClickListener {
                startActivity(Intent(this@GateOverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra("TRIGGER_QUESTION", true)
                })
            }
        }
        card.addView(shield)
        card.addView(title)
        card.addView(subtitle)
        card.addView(button)
        root.addView(card)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        runCatching { wm.addView(root, params) }.onSuccess { overlayView = root }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        runCatching { windowManager?.removeView(view) }
        overlayView = null
        stopSelf()
    }
}
