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
import com.example.padeltracker.R
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.data.MatchRecord // Βάλε αυτό ψηλά στα imports αν δεν υπάρχει

@Composable
fun LiveScoreScreen(
    setup: MatchSetup,
    onFinish: (MatchRecord) -> Unit // <-- Η ΑΛΛΑΓΗ ΕΙΝΑΙ ΕΔΩ! Προσθέσαμε το MatchRecord
) {
    // NOTE FOR THE FUTURE:
    // The score and timer will be updated dynamically from the ViewModel
    // which communicates with the Wear OS watch.
    // For now (UI Design phase), we use static placeholder values.
    val scoreTeamA = "0"
    val scoreTeamB = "0"
    val timeString = "00:00"

    // Match status (waiting for the watch to initiate the match)
    val matchStatusText = "Waiting for watch to start..."

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
                .background(Color.Black.copy(alpha = 0.6f))
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
                fontSize = 56.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(50.dp))

            // --- Team A Section ---
            Text(
                text = setup.teamA.players.joinToString(" & ") { it.name },
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = scoreTeamA,
                color = Color.White,
                fontSize = 80.sp, // Enlarged font since buttons were removed
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(20.dp))

            // "VS" Divider
            Text("VS", color = Color.Gray, fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(20.dp))

            // --- Team B Section ---
            Text(
                text = setup.teamB.players.joinToString(" & ") { it.name },
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = scoreTeamB,
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(50.dp)) // Fixed spacing instead of pushing to the bottom

            // --- DEV HACK: Fake Watch Signal Button ---
            // KEEP THIS ONLY for UI development to navigate to the next screen.
            // It simulates the "Match Finished" signal from the watch.
            // --- DEV HACK: Fake Watch Signal Button ---
            OutlinedButton(
                onClick = {
                    // Φτιάχνουμε έναν εικονικό αγώνα με τα ονόματα των ομάδων από το setup!
                    val dummyMatch = MatchRecord(
                        date = "13/05/2026",
                        duration = "1h 30m",
                        score = "6-4, 4-6, 10-8",
                        avgHeartRate = 142,
                        forehands = 45,
                        backhands = 30,
                        forehandLobs = 12,
                        backhandLobs = 8,
                        smashes = 18,
                        services = 50,
                        teamAPlayers = setup.teamA.players.joinToString(" & ") { it.name },
                        teamBPlayers = setup.teamB.players.joinToString(" & ") { it.name },
                        winner = "Team A" // Ας πούμε ότι κέρδισε η ομάδα Α για το τεστ
                    )
                    // Τον στέλνουμε πίσω για αποθήκευση!
                    onFinish(dummyMatch)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = null
            ) {
                Text("DEV MODE: Simulate Watch Finish ->")
            }
        }
    }
}