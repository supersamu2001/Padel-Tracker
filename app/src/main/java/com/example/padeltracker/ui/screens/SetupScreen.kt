package com.example.padeltracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.padeltracker.data.MatchPreferences
import com.example.padeltracker.shared.*
import com.example.padeltracker.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(onBackClick: () -> Unit, onSendToWatch: (MatchSetup) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { MatchPreferences(context) }

    // Intercept system back button to go back to Home
    BackHandler {
        onBackClick()
    }

    // Define our custom Red color for reuse
    val activeRed = Color(0xFFD32F2F)

    // States for inputs - Initialized as empty strings for a fresh start every time
    var tournamentName by remember { mutableStateOf("") }
    var tAP1 by remember { mutableStateOf("") }
    var tAP2 by remember { mutableStateOf("") }
    var tBP1 by remember { mutableStateOf("") }
    var tBP2 by remember { mutableStateOf("") }

    // The form is valid only when the tournament name and all 4 players are filled
    val isFormValid = tournamentName.isNotBlank() &&
            tAP1.isNotBlank() && tAP2.isNotBlank() &&
            tBP1.isNotBlank() && tBP2.isNotBlank()

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Full-screen background image
        Image(
            painter = painterResource(id = R.drawable.setup),
            contentDescription = "Setup screen background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle dark overlay to improve text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // 2. Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = activeRed // Back arrow is Red
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "Create new\ntournament",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 40.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Fill in the details to create a new padel tournament 🎾",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                /**
                // Tournament Name Card
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("NAME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        OutlinedTextField(
                            value = tournamentName,
                            onValueChange = { tournamentName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Monday tournament", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = activeRed, // Red border when focused
                                focusedLabelColor = activeRed,  // Red label when focused
                                cursorColor = activeRed         // Red cursor
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                */

                Text(
                    "PLAYERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Team A Card
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Team A", color = DarkTeal, fontWeight = FontWeight.Bold)

                        // Player 1
                        OutlinedTextField(
                            value = tAP1,
                            onValueChange = { tAP1 = it },
                            label = { Text("Player 1 Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = activeRed,
                                focusedLabelColor = activeRed,
                                cursorColor = activeRed
                            )
                        )

                        // Player 2
                        OutlinedTextField(
                            value = tAP2,
                            onValueChange = { tAP2 = it },
                            label = { Text("Player 2 Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = activeRed,
                                focusedLabelColor = activeRed,
                                cursorColor = activeRed
                            )
                        )
                    }
                }

                // Team B Card
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Team B", color = PadelLimeGreen, fontWeight = FontWeight.Bold)

                        // Player 1
                        OutlinedTextField(
                            value = tBP1,
                            onValueChange = { tBP1 = it },
                            label = { Text("Player 1 Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = activeRed,
                                focusedLabelColor = activeRed,
                                cursorColor = activeRed
                            )
                        )

                        // Player 2
                        OutlinedTextField(
                            value = tBP2,
                            onValueChange = { tBP2 = it },
                            label = { Text("Player 2 Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = activeRed,
                                focusedLabelColor = activeRed,
                                cursorColor = activeRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Action Button
                Button(
                    enabled = isFormValid,
                    onClick = {
                        // Save the names internally just in case other parts of the app need them
                        scope.launch {
                            prefs.savePlayerNames(tAP1, tAP2, tBP1, tBP2)
                        }

                        val setup = MatchSetup(
                            matchId = UUID.randomUUID().toString(),
                            teamA = TeamSetup(
                                id = "team_a",
                                name = "Team A",
                                players = listOf(
                                    PlayerSetup(id = "a1", name = tAP1),
                                    PlayerSetup(id = "a2", name = tAP2)
                                )
                            ),
                            teamB = TeamSetup(
                                id = "team_b",
                                name = "Team B",
                                players = listOf(
                                    PlayerSetup(id = "b1", name = tBP1),
                                    PlayerSetup(id = "b2", name = tBP2)
                                )
                            ),
                            rules = MatchRules(),
                            createdAt = System.currentTimeMillis()
                        )
                        onSendToWatch(setup)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkTeal, disabledContainerColor = Color.LightGray),
                    shape = CircleShape
                ) {
                    Text("Send to Watch", fontWeight = FontWeight.Bold, color = if (isFormValid) White else Color.DarkGray)
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}