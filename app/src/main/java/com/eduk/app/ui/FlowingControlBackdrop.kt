package com.eduk.app.ui

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext

@Composable
fun FlowingControlBackdrop(pulse: Int, modifier: Modifier = Modifier) {
    val motionEnabled = rememberEdukMotionEnabled()
    val transition = rememberInfiniteTransition(label = "eduk-flow")
    val animatedDrift by transition.animateFloat(
        initialValue = -0.05f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(11_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eduk-flow-drift"
    )
    val animatedAccent by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.43f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eduk-flow-accent"
    )
    val drift = if (motionEnabled) animatedDrift else 0.5f
    val accent = if (motionEnabled) animatedAccent else 0.18f
    val animatedPulse by animateFloatAsState(
        targetValue = pulse.toFloat(),
        animationSpec = tween(if (motionEnabled) 620 else 0, easing = FastOutSlowInEasing),
        label = "eduk-flow-pulse"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE7F0FF),
                    Color(0xFFF8F2FF),
                    Color(0xFFFFF0E2)
                )
            )
        )
        drawCircle(
            color = Color(0xFF2E6CB8).copy(alpha = 0.11f + accent * 0.08f),
            radius = size.width * (0.36f + accent * 0.07f),
            center = androidx.compose.ui.geometry.Offset(size.width * (0.94f - drift * 0.18f), size.height * 0.12f)
        )
        drawCircle(
            color = Color(0xFFFF7A1A).copy(alpha = 0.10f + accent * 0.09f),
            radius = size.width * (0.29f + accent * 0.06f),
            center = androidx.compose.ui.geometry.Offset(size.width * (0.10f + drift * 0.12f), size.height * 0.86f)
        )
        drawCircle(
            color = Color(0xFF7450C8).copy(alpha = 0.07f + accent * 0.06f),
            radius = size.width * 0.22f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.70f, size.height * (0.54f + drift * 0.08f))
        )
        val pulseShift = (animatedPulse % 7f) * 30f
        fun flowPath(baseY: Float, amplitude: Float, shift: Float): Path = Path().apply {
            moveTo(-size.width * 0.15f, baseY)
            cubicTo(
                size.width * 0.18f, baseY - amplitude,
                size.width * 0.49f, baseY + amplitude,
                size.width * 0.74f, baseY - amplitude * 0.3f
            )
            cubicTo(
                size.width * 0.95f, baseY - amplitude * 0.8f,
                size.width * 1.1f, baseY + amplitude * 0.24f,
                size.width * 1.2f, baseY + shift
            )
        }
        drawPath(
            flowPath(size.height * (0.13f + drift * 0.06f), size.width * 0.18f, pulseShift),
            color = Color(0xFF2E6CB8).copy(alpha = 0.14f + accent * 0.15f),
            style = Stroke(width = 5.2f)
        )
        drawPath(
            flowPath(size.height * (0.46f - drift * 0.05f), size.width * 0.24f, -pulseShift),
            color = Color(0xFFFF7A1A).copy(alpha = 0.16f + accent * 0.16f),
            style = Stroke(width = 5.8f)
        )
        drawPath(
            flowPath(size.height * (0.82f + drift * 0.03f), size.width * 0.14f, pulseShift * 0.4f),
            color = Color(0xFF7450C8).copy(alpha = 0.11f + accent * 0.13f),
            style = Stroke(width = 4.5f)
        )
    }
}

@Composable
fun rememberEdukMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }
}
