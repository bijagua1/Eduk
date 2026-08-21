package com.eduk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val VisualNavy = Color(0xFF0B1F3A)
private val VisualDeepBlue = Color(0xFF173D6C)
private val VisualOrange = Color(0xFFFF7A1A)

@Composable
fun EdukGlowHero(
    overline: String,
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(VisualNavy, VisualDeepBlue, VisualNavy)))
                .padding(24.dp)
        ) {
            Surface(
                color = VisualOrange.copy(alpha = 0.17f),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.align(Alignment.TopEnd).size(98.dp)
            ) {}
            Surface(
                color = Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.align(Alignment.BottomEnd).size(64.dp)
            ) {}
            Column {
                Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = VisualOrange, modifier = Modifier.size(27.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(overline.uppercase(), color = VisualOrange, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text(description, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun EdukStepPill(label: String, modifier: Modifier = Modifier) {
    Surface(color = VisualOrange.copy(alpha = 0.12f), shape = RoundedCornerShape(99.dp), modifier = modifier) {
        Text(label.uppercase(), color = Color(0xFF9A4F17), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
    }
}
