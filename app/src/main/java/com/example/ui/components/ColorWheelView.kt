package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun ColorWheelView(
    title: String,
    hue: Float,
    amount: Float,
    onHueAmountChange: (Float, Float) -> Unit,
    wheelSize: androidx.compose.ui.unit.Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(wheelSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touch = change.position
                            val dx = touch.x - center.x
                            val dy = touch.y - center.y
                            val radius = size.width / 2f

                            val dist = sqrt(dx * dx + dy * dy)
                            val normalizedAmount = (dist / radius * 100f).coerceIn(0f, 100f)

                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            onHueAmountChange(angle, normalizedAmount)
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Draw rainbow gradient disc
                val colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                    Color.Blue, Color.Magenta, Color.Red
                )
                drawCircle(
                    brush = Brush.sweepGradient(colors, center),
                    radius = radius
                )
                // Desaturate center with radial white overlay
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )
                // Border ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = 2f)
                )

                // Puck indicator
                val puckDist = (amount / 100f) * radius
                val rad = Math.toRadians(hue.toDouble())
                val puckX = center.x + (puckDist * cos(rad)).toFloat()
                val puckY = center.y + (puckDist * sin(rad)).toFloat()

                drawCircle(
                    color = Color.Black,
                    radius = 8f,
                    center = Offset(puckX, puckY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(puckX, puckY)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (amount > 0) "${amount.roundToInt()}%" else "Neutral",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
