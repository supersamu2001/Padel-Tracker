package com.example.padeltracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.R
import com.example.padeltracker.data.MatchRecord
import com.example.padeltracker.shared.MatchSetup
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface

@Composable
fun GameAnalysisScreen(
    record: MatchRecord?,
    setup: MatchSetup?,
    onGoHome: () -> Unit
) {
    val scrollState = rememberScrollState()
    val activeRed = Color(0xFFD32F2F)

    val teamANames = record?.teamAPlayers ?: setup?.teamA?.players?.joinToString(" & ") { it.name } ?: "Team A"
    val teamBNames = record?.teamBPlayers ?: setup?.teamB?.players?.joinToString(" & ") { it.name } ?: "Team B"
    val displayScore = record?.score ?: "Match Data"

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.statistics),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))
            Text("MATCH ANALYSIS", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                text = if (record != null) "Date: ${record.date}" else "Live Performance",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SCORE BOARD
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FINAL RESULT", color = activeRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(teamANames, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(teamBNames, color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                        }
                        Text(text = displayScore, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PHYSICAL STATS - All mock values removed
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val avgHr = if (record?.avgHeartRate != null && record.avgHeartRate > 0) record.avgHeartRate.toString() else "0"
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "AVG HEART RATE",
                    value = avgHr,
                    unit = "BPM",
                    icon = "❤️"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "DURATION",
                    value = record?.duration ?: "0",
                    unit = "MIN",
                    icon = "⏱️"
                )
            }

            // HEART RATE GRAPH
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("HEART RATE ZONES", color = activeRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val actualHistory = record?.heartRateHistory ?: ""

                    // MODIFIED: Passed the duration down to the graph for the X axis
                    HeartRateGraph(
                        historyStr = actualHistory,
                        durationStr = record?.duration,
                        color = activeRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TECHNICAL PERFORMANCE - All mock values removed
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("SHOT ANALYSIS", color = activeRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    ShotRow("Forehands", record?.forehands ?: 0, Color(0xFFDEFF9A))
                    Spacer(modifier = Modifier.height(12.dp))
                    ShotRow("Backhands", record?.backhands ?: 0, Color(0xFF00BCD4))
                    Spacer(modifier = Modifier.height(12.dp))
                    ShotRow("Smashes", record?.smashes ?: 0, activeRed)
                    Spacer(modifier = Modifier.height(12.dp))
                    ShotRow("Services", record?.services ?: 0, Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    ShotRow("Forehand Lobs", record?.forehandLobs ?: 0, Color.Yellow)
                    Spacer(modifier = Modifier.height(12.dp))
                    ShotRow("Backhand Lobs", record?.backhandLobs ?: 0, Color.Cyan)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = activeRed), shape = RoundedCornerShape(50.dp)) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DONE", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}


// heartbeat graphs
@Composable
fun HeartRateGraph(historyStr: String, durationStr: String?, color: Color) {
    val points = historyStr.split(",").mapNotNull { it.trim().toFloatOrNull() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // MODIFIED: Increased height slightly to accommodate the text axes
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        if (points.size < 2) {
            Text(
                text = "No heart rate data recorded for this match",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // ADDED: Padding to ensure text doesn't overlap with the edges
                val paddingLeft = 35.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val paddingTop = 10.dp.toPx()
                val paddingRight = 15.dp.toPx()

                val graphWidth = width - paddingLeft - paddingRight
                val graphHeight = height - paddingTop - paddingBottom

                val maxBpm = points.maxOrNull() ?: 180f
                val minBpm = points.minOrNull() ?: 60f

                // ADDED: Calculate range with a small buffer so lines don't hit the absolute ceiling/floor
                val bpmRange = if (maxBpm == minBpm) 1f else (maxBpm - minBpm) * 1.2f
                val baseMinBpm = minBpm - (bpmRange * 0.1f)

                // 1. DRAWING THE GRAPH LINE
                val path = Path()
                val pointSpacing = graphWidth / (points.size - 1)

                points.forEachIndexed { index, bpm ->
                    val x = paddingLeft + (index * pointSpacing)
                    val normalizedY = 1f - ((bpm - baseMinBpm) / bpmRange)
                    val y = paddingTop + (normalizedY * graphHeight)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 2. DRAWING THE AXIS TEXT (Numbers only)
                val textPaint = Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    alpha = (255 * 0.5f).toInt() // Semi-transparent white
                    textSize = 10.dp.toPx()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas

                    // ADDED: Y-Axis (Top, Middle, Bottom values - Numbers ONLY)
                    nativeCanvas.drawText("${maxBpm.toInt()}", paddingLeft - 8.dp.toPx(), paddingTop + 4.dp.toPx(), textPaint)
                    nativeCanvas.drawText("${((maxBpm + minBpm) / 2).toInt()}", paddingLeft - 8.dp.toPx(), paddingTop + (graphHeight / 2) + 4.dp.toPx(), textPaint)
                    nativeCanvas.drawText("${minBpm.toInt()}", paddingLeft - 8.dp.toPx(), paddingTop + graphHeight + 4.dp.toPx(), textPaint)

                    // ADDED: X-Axis (Time from 0 to Match Duration)
                    textPaint.textAlign = Paint.Align.LEFT
                    nativeCanvas.drawText("0", paddingLeft, height - 2.dp.toPx(), textPaint)

                    val duration = durationStr ?: "0"
                    textPaint.textAlign = Paint.Align.RIGHT
                    nativeCanvas.drawText("$duration min", width - paddingRight, height - 2.dp.toPx(), textPaint)
                }
            }
        }
    }
}

// ... helper composables (StatCard, ShotRow) same as before ...
@Composable
fun StatCard(modifier: Modifier, label: String, value: String, unit: String, icon: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(unit, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShotRow(label: String, count: Int, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text(count.toString(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (count > 0) (count / 60f).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}