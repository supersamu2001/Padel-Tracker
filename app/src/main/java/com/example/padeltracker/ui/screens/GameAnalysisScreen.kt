package com.example.padeltracker.ui.screens

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.drawToBitmap
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.padeltracker.R
import com.example.padeltracker.data.MatchRecord
import com.example.padeltracker.shared.MatchSetup
import androidx.compose.foundation.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ComposeView
import android.content.Context
import android.content.Intent
import androidx.core.view.drawToBitmap
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


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

    // Logic to clean the score: remove the third set if it is 0-0
    val rawScore = record?.score ?: "Match Data"
    val displayScore = if (rawScore.endsWith(", 0-0")) {
        rawScore.removeSuffix(", 0-0")
    } else if (rawScore.endsWith(" 0-0")) {
        rawScore.removeSuffix(" 0-0")
    } else {
        rawScore
    }
    

    // Required tools for taking the screenshot and sharing it
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var redrawCounter by remember { mutableIntStateOf(0) }

    BackHandler {
        onGoHome()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (record != null) {
            // key(redrawCounter) forces a total re-render of this block when redrawCounter changes
            key(redrawCounter) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .drawWithContent {
                            // Record the visual output to our graphics layer for sharing
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            // Draw normally so Compose keeps the layer "alive"
                            drawContent()
                        }
                ) {
                    MatchSummaryShareCard(record = record, activeRed = activeRed)
                }
            }
        }

        // VISIBLE BACKGROUND
        Image(
            painter = painterResource(id = R.drawable.statistics),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))

        // THE ACTUAL VISIBLE UI
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text("MATCH ANALYSIS", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)

                IconButton(onClick = {
                    if (record != null) {
                        coroutineScope.launch {
                            try {
                                // 1. Trigger a redraw of the hidden card in order to be sure that the image
                                // will be loaded correctly
                                redrawCounter++
                                // 2. Wait for the next frame to be rendered
                                kotlinx.coroutines.yield() 
                                // 3. Take the screenshot
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                shareBitmap(context, bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share All Results", tint = Color.White)
                }
            }

            Text(
                text = if (record != null) "Date: ${record.date}" else "Live Performance",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SCORE BOARD
            Card(
                // Reverted modifier to standard. Removed drawWithContent as we no longer screenshot this element
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FINAL RESULT", color = activeRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            val isTeamAWinner = record?.winner == "Team A"
                            val isTeamBWinner = record?.winner == "Team B"

                            Text(
                                text = teamANames,
                                color = if (isTeamAWinner || record == null) Color.White else Color.White.copy(alpha = 0.5f),
                                fontWeight = if (isTeamAWinner) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = teamBNames,
                                color = if (isTeamBWinner || record == null) Color.White else Color.White.copy(alpha = 0.5f),
                                fontWeight = if (isTeamBWinner) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(text = displayScore, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (record != null) {
                MatchBadges(record = record, activeRed = activeRed)
                Spacer(modifier = Modifier.height(20.dp))
            }

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

                    // Passed the duration down to the graph for the X axis
                    HeartRateGraph(
                        historyStr = actualHistory,
                        durationStr = record?.duration,
                        color = activeRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TECHNICAL PERFORMANCE
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
            // Increased height slightly to accommodate the text axes
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

                //  Padding to ensure text doesn't overlap with the edges
                val paddingLeft = 35.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val paddingTop = 10.dp.toPx()
                val paddingRight = 15.dp.toPx()

                val graphWidth = width - paddingLeft - paddingRight
                val graphHeight = height - paddingTop - paddingBottom

                val maxBpm = points.maxOrNull() ?: 180f
                val minBpm = points.minOrNull() ?: 60f

                // Calculate range with a small buffer so lines don't hit the absolute ceiling/floor
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

                    // Y-Axis (Top, Middle, Bottom values - Numbers ONLY)
                    nativeCanvas.drawText("${maxBpm.toInt()}", paddingLeft - 8.dp.toPx(), paddingTop + 4.dp.toPx(), textPaint)
                    nativeCanvas.drawText("${((maxBpm + minBpm) / 2).toInt()}", paddingLeft - 8.dp.toPx(), paddingTop + (graphHeight / 2) + 4.dp.toPx(), textPaint)
                    nativeCanvas.drawText("${minBpm.toInt()}", paddingLeft - 8.dp.toPx(), paddingTop + graphHeight + 4.dp.toPx(), textPaint)

                    // X-Axis (Time from 0 to Match Duration)
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

// MatchBadges composable to display dynamic achievements based on match data
@Composable
fun MatchBadges(record: MatchRecord, activeRed: Color) {
    val badges = mutableListOf<String>()

    // 1. Duration Check
    val durationInt = record.duration.toIntOrNull() ?: 0
    if (durationInt >= 90) badges.add("⏱️ ENDURANCE")

    // 2. Heart Rate Check
    if (record.avgHeartRate >= 140) badges.add("🫀 HIGH INTENSITY")

    // 3. Smashes Check
    if (record.smashes >= 10) badges.add("🔥 AGGRESSOR")

    // 4. Lobs (Tactics) Check
    val totalLobs = record.forehandLobs + record.backhandLobs
    if (totalLobs >= 15) badges.add("🛡️ TACTICIAN")

    // Draw badges if any were earned
    if (badges.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            badges.forEach { badgeText ->
                Box(
                    modifier = Modifier
                        .background(
                            color = activeRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, activeRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = activeRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * Function that implements the UI of the image created when we want to share the match
 */
@Composable
fun MatchSummaryShareCard(record: MatchRecord, activeRed: Color) {
    // Logic to clean the score: remove the third set if it is 0-0
    val rawScore = record.score
    val displayScore = if (rawScore.endsWith(", 0-0")) {
        rawScore.removeSuffix(", 0-0")
    } else if (rawScore.endsWith(" 0-0")) {
        rawScore.removeSuffix(" 0-0")
    } else {
        rawScore
    }

    // Root container changed from Column to Box to allow background image
    Box(
        modifier = Modifier
            .width(360.dp) // Fixed width for consistent image quality on share
            .background(Color.Black)
    ) {

        Image(
            painter = painterResource(id = R.drawable.share),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f)))


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp), // Overall padding for the card content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding & Date
            Text("PADEL TRACKER", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = activeRed)
            Text("Match Summary • ${record.date}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)), // Adjusted alpha for overlay
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val isTeamAWinner = record.winner == "Team A"
                    val isTeamBWinner = record.winner == "Team B"

                    Text(
                        text = record.teamAPlayers ?: "Team A",
                        color = if (isTeamAWinner) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (isTeamAWinner) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "vs",
                        color = activeRed.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = record.teamBPlayers ?: "Team B",
                        color = if (isTeamBWinner) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (isTeamBWinner) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = displayScore,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges
            MatchBadges(record = record, activeRed = activeRed)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            // Physical Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ShareStatItem(label = "AVG HEART RATE", value = "${record.avgHeartRate}", unit = "BPM", color = activeRed)
                ShareStatItem(label = "DURATION", value = record.duration, unit = "MIN", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            // Heart Rate Graph Integration
            Text("HEART RATE TREND", color = activeRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            // Calling existing HeartRateGraph helper, color slightly desaturated for background image
            HeartRateGraph(
                historyStr = record.heartRateHistory ?: "",
                durationStr = record.duration,
                color = activeRed.copy(alpha = 0.8f)
            )

            // Extra divider between graph and shots
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            // Shots Analysis Summary
            Text("SHOTS ANALYSIS", color = activeRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))

            // Showing ALL 6 shot types individually (reverted simplification)
            ShareShotRow(label = "Smashes", count = record.smashes, activeRed)
            Spacer(modifier = Modifier.height(8.dp))
            // Colors matched from GameAnalysisScreen
            ShareShotRow(label = "Forehands", count = record.forehands, Color(0xFFDEFF9A))
            Spacer(modifier = Modifier.height(8.dp))
            ShareShotRow(label = "Backhands", count = record.backhands, Color(0xFF00BCD4))
            Spacer(modifier = Modifier.height(8.dp))
            ShareShotRow(label = "Services", count = record.services, Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            ShareShotRow(label = "Forehand Lobs", count = record.forehandLobs, Color.Yellow)
            Spacer(modifier = Modifier.height(8.dp))
            ShareShotRow(label = "Backhand Lobs", count = record.backhandLobs, Color.Cyan)

            Spacer(modifier = Modifier.height(24.dp))

            // Footer (same as before)
            Text("Tracked with Pixel Watch", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
        }
    }
}

// Compact helper Composable for the physical stats in the Share Card
@Composable
fun ShareStatItem(label: String, value: String, unit: String, color: Color) {
    Column {
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("$label ($unit)", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// Compact helper Composable for the shot analysis rows in the Share Card
@Composable
fun ShareShotRow(label: String, count: Int, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
        Text(count.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}



// Function to save the bitmap to cache and trigger the Android Share Intent
fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        // Changed file name to represent full analysis
        val file = File(cachePath, "match_analysis_full.png")

        val stream = FileOutputStream(file)
        //Slightly adjusted compression for better quality of the large image
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        stream.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Padel Match Full Analysis")
            putExtra(Intent.EXTRA_TEXT, "Detailed statistics from my last padel match!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "Padel Stats", uri)
        }
        context.startActivity(Intent.createChooser(intent, "Share Match Stats!"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}