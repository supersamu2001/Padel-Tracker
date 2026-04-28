package com.example.padeltracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.R
import com.example.padeltracker.data.AppDatabase
import com.example.padeltracker.data.MatchRecord
import com.example.padeltracker.shared.MatchConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LiveScoreScreen(
    config: MatchConfig,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // State for the score counters
    var scoreTeamA by remember { mutableStateOf(0) }
    var scoreTeamB by remember { mutableStateOf(0) }

    // State for the live timer (in seconds)
    var timeInSeconds by remember { mutableStateOf(0) }

    // LaunchedEffect starts the timer as soon as the screen is displayed
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L) // Wait for exactly 1 second
            timeInSeconds++
        }
    }

    // Convert total seconds into a Minutes:Seconds format (e.g., "12:05")
    val minutes = timeInSeconds / 60
    val seconds = timeInSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    // Main container (Box allows us to stack UI elements on top of the background image)
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. The Background Image
        Image(
            painter = painterResource(id = R.drawable.green_balls), // Ensure your image is named correctly in the res/drawable folder
            contentDescription = "Padel balls background",
            contentScale = ContentScale.Crop, // Stretches the image to fill the entire screen
            modifier = Modifier.fillMaxSize()
        )

        // 2. A semi-transparent black overlay to make the white text readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // 3. The actual UI elements (Score, Timer, and Buttons)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Live Timer Display
            Text(
                text = timeString,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- Team A Section ---
            Text(
                text = "${config.teamAPlayer1} & ${config.teamAPlayer2}",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "$scoreTeamA",
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { scoreTeamA++ }) {
                Text("+1 Point")
            }

            Spacer(modifier = Modifier.height(30.dp))
            Text("VS", color = Color.Gray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(30.dp))

            // --- Team B Section ---
            Text(
                text = "${config.teamBPlayer1} & ${config.teamBPlayer2}",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "$scoreTeamB",
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { scoreTeamB++ }) {
                Text("+1 Point")
            }

            Spacer(modifier = Modifier.height(50.dp))

            // --- Finish Match Button ---
            Button(
                onClick = {
                    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                    // Create the match record using the actual timer duration
                    val record = MatchRecord(
                        date = currentDate,
                        duration = timeString,
                        score = "$scoreTeamA - $scoreTeamB",
                        avgHeartRate = 0, // Placeholder until watch integration
                        forehands = 0,    // Placeholder until watch integration
                        backhands = 0,    // Placeholder until watch integration
                        teamAPlayers = "${config.teamAPlayer1}, ${config.teamAPlayer2}",
                        teamBPlayers = "${config.teamBPlayer1}, ${config.teamBPlayer2}"
                    )

                    // Execute database insertion on a background thread (IO)
                    scope.launch(Dispatchers.IO) {
                        db.matchDao().insertMatch(record)

                        // Switch back to the Main thread to handle UI navigation
                        withContext(Dispatchers.Main) {
                            onFinish()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Finish & Save Match")
            }
        }
    }
}