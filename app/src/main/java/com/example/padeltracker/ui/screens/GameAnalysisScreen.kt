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
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "AVG HEART RATE",
                    value = record?.avgHeartRate?.toString() ?: "0",
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

            // heartbeat graph
            if (record != null && record.heartRateHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("HEART RATE ZONES", color = activeRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        HeartRateGraph(historyStr = record.heartRateHistory, color = activeRed)
                    }
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
fun HeartRateGraph(historyStr: String, color: Color) {
    // 1. Μετατροπή του "110,120,135" σε λίστα με νούμερα [110, 120, 135]
    val points = historyStr.split(",").mapNotNull { it.trim().toFloatOrNull() }

    if (points.size < 2) {
        Text("Not enough data to draw graph", color = Color.Gray, fontSize = 12.sp)
        return
    }

    val maxBpm = points.maxOrNull() ?: 180f
    val minBpm = (points.minOrNull() ?: 60f) - 10f // Λίγος αέρας από κάτω

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height
        val pointSpacing = width / (points.size - 1)

        val path = Path()

        points.forEachIndexed { index, bpm ->
            val x = index * pointSpacing
            // Υπολογισμός του Y ώστε να ταιριάζει στο ύψος (τα μεγάλα νούμερα πάνε πάνω)
            val normalizedY = 1f - ((bpm - minBpm) / (maxBpm - minBpm))
            val y = normalizedY * height

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
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
// end heartbeat

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