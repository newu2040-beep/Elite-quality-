package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CurvesGraphView(
    curveValue: Float, // -100 to 100
    onCurveValueChange: (Float) -> Unit,
    channelColor: Color = Color.White,
    channelName: String = "RGB Master",
    graphHeight: androidx.compose.ui.unit.Dp = 130.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = channelName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = channelColor
            )
            Text(
                text = if (curveValue >= 0) "+${curveValue.toInt()}" else "${curveValue.toInt()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(graphHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = -(dragAmount.y / size.height) * 200f
                            onCurveValueChange((curveValue + delta).coerceIn(-100f, 100f))
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw grid lines
                val gridColor = Color.White.copy(alpha = 0.12f)
                drawLine(gridColor, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), 1f)
                drawLine(gridColor, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), 1f)
                drawLine(gridColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), 1f)
                drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), 1f)
                drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)
                drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), 1f)

                // Diagonal reference line
                drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, h), Offset(w, 0f), 1f)

                // Midpoint control
                val controlY = (h * 0.5f) - (curveValue / 100f) * (h * 0.4f)
                val path = Path().apply {
                    moveTo(0f, h)
                    quadraticTo(w * 0.5f, controlY, w, 0f)
                }

                drawPath(
                    path = path,
                    color = channelColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Midpoint handle circle
                drawCircle(
                    color = channelColor,
                    radius = 6.dp.toPx(),
                    center = Offset(w * 0.5f, controlY)
                )
            }
        }
    }
}
