package com.eduk.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun FlowingControlBackdrop(pulse: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eduk-flow")
    val drift by transition.animateFloat(
        initialValue = -0.05f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(11_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eduk-flow-drift"
    )
    val accent by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.43f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eduk-flow-accent"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val pulseShift = (pulse % 7) * 18f
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
            color = Color(0xFF2E6CB8).copy(alpha = 0.06f + accent * 0.08f),
            style = Stroke(width = 2.2f)
        )
        drawPath(
            flowPath(size.height * (0.46f - drift * 0.05f), size.width * 0.24f, -pulseShift),
            color = Color(0xFFFF7A1A).copy(alpha = 0.07f + accent * 0.09f),
            style = Stroke(width = 2.6f)
        )
        drawPath(
            flowPath(size.height * (0.82f + drift * 0.03f), size.width * 0.14f, pulseShift * 0.4f),
            color = Color(0xFF0B1F3A).copy(alpha = 0.05f + accent * 0.06f),
            style = Stroke(width = 1.8f)
        )
    }
}
