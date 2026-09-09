package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CurvesGraphView(
    curveValue: Float, // -100 to 100
    onCurveValueChange: (Float) -> Unit,
    channelColor: Color = Color.White,
    channelName: String = "RGB Master",
    graphHeight: Dp = 130.dp,
    modifier: Modifier = Modifier
) {
    val currentCurveVal by rememberUpdatedState(curveValue)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(channelColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (curveValue >= 0) "+${curveValue.toInt()}%" else "${curveValue.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = channelColor
                )

                if (curveValue != 0f) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onCurveValueChange(0f) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset Curve",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(graphHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.65f))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val h = size.height.toFloat()
                            val normalizedY = (offset.y / h).coerceIn(0.1f, 0.9f)
                            val targetVal = ((0.5f - normalizedY) / 0.4f) * 100f
                            onCurveValueChange(targetVal.coerceIn(-100f, 100f))
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val h = size.height.toFloat()
                            val delta = -(dragAmount.y / h) * 180f
                            val newVal = (currentCurveVal + delta).coerceIn(-100f, 100f)
                            onCurveValueChange(newVal)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw 4x4 Grid
                val gridColor = Color.White.copy(alpha = 0.10f)
                drawLine(gridColor, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), 1f)
                drawLine(gridColor, Offset(w * 0.50f, 0f), Offset(w * 0.50f, h), 1f)
                drawLine(gridColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), 1f)
                drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), 1f)
                drawLine(gridColor, Offset(0f, h * 0.50f), Offset(w, h * 0.50f), 1f)
                drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), 1f)

                // Linear diagonal reference line
                drawLine(
                    Color.White.copy(alpha = 0.25f),
                    Offset(0f, h),
                    Offset(w, 0f),
                    strokeWidth = 1.dp.toPx()
                )

                // Smooth S-Curve Path with 3-point Bézier interpolation
                val controlY = (h * 0.5f) - (currentCurveVal / 100f) * (h * 0.42f)
                val path = Path().apply {
                    moveTo(0f, h)
                    cubicTo(
                        w * 0.25f, h * 0.75f - (currentCurveVal / 100f) * (h * 0.25f),
                        w * 0.75f, h * 0.25f - (currentCurveVal / 100f) * (h * 0.25f),
                        w, 0f
                    )
                }

                // Shaded region under curve
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    color = channelColor.copy(alpha = 0.12f)
                )

                // Main Curve Stroke
                drawPath(
                    path = path,
                    color = channelColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Interactive midpoint handle point
                val midX = w * 0.5f
                val midY = (h * 0.5f) - (currentCurveVal / 100f) * (h * 0.25f)
                drawCircle(
                    color = Color.Black,
                    radius = 8.dp.toPx(),
                    center = Offset(midX, midY)
                )
                drawCircle(
                    color = channelColor,
                    radius = 6.dp.toPx(),
                    center = Offset(midX, midY)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Precision adjustment slider
        Slider(
            value = curveValue,
            onValueChange = onCurveValueChange,
            valueRange = -100f..100f,
            colors = SliderDefaults.colors(
                thumbColor = channelColor,
                activeTrackColor = channelColor,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
