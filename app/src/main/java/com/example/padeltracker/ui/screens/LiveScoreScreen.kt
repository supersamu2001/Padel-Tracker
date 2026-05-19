package com.example.padeltracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.padeltracker.R
import com.example.padeltracker.shared.MatchSetup
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import java.util.Locale
import com.example.padeltracker.data.MatchRecord

@Composable
fun LiveScoreScreen(
    setup: MatchSetup,
    onFinish: () -> Unit
) {

    val context = LocalContext.current

    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    var liveScoreString by remember { mutableStateOf("0-0") }
    var matchStatusText by remember { mutableStateOf("Waiting for watch to start...") }

    // NEW: State to control whether the timer should tick or stay frozen
    var isMatchStarted by remember { mutableStateOf(false) }

    // The timer now only ticks IF isMatchStarted is true
    LaunchedEffect(isMatchStarted) {
        if (isMatchStarted) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Format the timer
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    // Listen for live score updates from the watch via MessageClient
    DisposableEffect(Unit) {
        val messageClient = Wearable.getMessageClient(context)
        val messageListener = MessageClient.OnMessageReceivedListener { messageEvent: MessageEvent ->
            when (messageEvent.path) {
                // Listen for the match start signal from the watch
                "/match_started" -> {
                    isMatchStarted = true
                    matchStatusText = "Match in progress..."
                }
                // Listen for score updates
                "/live_score" -> {
                    val newScore = String(messageEvent.data)
                    liveScoreString = newScore
                }
            }
        }
        messageClient.addListener(messageListener)

        onDispose {
            messageClient.removeListener(messageListener)
        }
    }

    val teamAName = setup.teamA.players.joinToString(" & ") { it.name }
    val teamBName = setup.teamB.players.joinToString(" & ") { it.name }

    // Main container
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Full-screen background image
        Image(
            painter = painterResource(id = R.drawable.green_balls),
            contentDescription = "Padel balls background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Semi-transparent black overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )

        // 3. Dashboard UI Content (Centered)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // This centers everything vertically
        ) {
            // Status Info Pill
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = matchStatusText,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Display (Passive, controlled by watch)
            Text(
                text = timeString,
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(50.dp))

            //new
            PadelScoreboard(
                teamAName = teamAName,
                teamBName = teamBName,
                scoreString = liveScoreString
            )

            Spacer(modifier = Modifier.height(50.dp))

        }
    }
}


@Composable
fun PadelScoreboard(teamAName: String, teamBName: String, scoreString: String) {
    // Split the incoming string like "6-4   1-0" into separate sets
    val sets = scoreString.split(Regex("\\s+")).filter { it.isNotBlank() }

    // Extract scores for Team A (left of the dash) and Team B (right of the dash)
    val teamAScores = sets.map { it.split("-").getOrNull(0) ?: "0" }
    val teamBScores = sets.map { it.split("-").getOrNull(1) ?: "0" }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // TEAM A ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = teamAName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f) // Pushes the score boxes to the right
                )
                teamAScores.forEachIndexed { index, score ->
                    ScoreBox(score = score, isActive = index == teamAScores.lastIndex)
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // TEAM B ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = teamBName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                teamBScores.forEachIndexed { index, score ->
                    ScoreBox(score = score, isActive = index == teamBScores.lastIndex)
                }
            }
        }
    }
}

@Composable
fun ScoreBox(score: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(42.dp)
            .background(
                // Active set is Red, finished sets are semi-transparent white
                color = if (isActive) Color(0xFFD32F2F) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp
        )
    }
}