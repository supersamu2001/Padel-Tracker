package com.example.padeltracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun TennisBallView(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.width / 2
        val strokeWidth = size.width * 0.08f

        // Draw the neon ball body with a radial gradient for 3D effect
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE6FF2E), Color(0xFFB4D300)),
                center = center,
                radius = radius
            ),
            radius = radius
        )

        // Draw the white seam paths (integrated onto the ball surface)
        val leftPath = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.25f)
            quadraticBezierTo(
                size.width * 0.45f, size.height * 0.5f,
                size.width * 0.15f, size.height * 0.75f
            )
        }
        drawPath(
            path = leftPath,
            color = Color.White.copy(alpha = 0.9f),
            style = Stroke(width = strokeWidth)
        )

        val rightPath = Path().apply {
            moveTo(size.width * 0.85f, size.height * 0.25f)
            quadraticBezierTo(
                size.width * 0.55f, size.height * 0.5f,
                size.width * 0.85f, size.height * 0.75f
            )
        }
        drawPath(
            path = rightPath,
            color = Color.White.copy(alpha = 0.9f),
            style = Stroke(width = strokeWidth)
        )
    }
}