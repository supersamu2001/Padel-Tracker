package com.example.padeltracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.shared.MatchConfig
import com.example.padeltracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(onBackClick: () -> Unit, onSendToWatch: (MatchConfig) -> Unit) {

    // --- 🌟 NEW: States for Tournament Details ---
    var tournamentName by remember { mutableStateOf(TextFieldValue("e.g. Monday tournament")) } // Pre-filled example
    val availablePoints = listOf(8, 16, 21, 24, 32, 40)
    var selectedPoints by remember { mutableStateOf(8) } // Default selection

    // Existing states for players
    var teamAPlayer1 by remember { mutableStateOf("") }
    var teamAPlayer2 by remember { mutableStateOf("") }
    var teamBPlayer1 by remember { mutableStateOf("") }
    var teamBPlayer2 by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp), // Space for bottom button
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("", fontWeight = FontWeight.Bold) }, // Keep title empty for clean look
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ClayOrange
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackgroundBeige,
                titleContentColor = DarkTeal
            )
        )

        // --- 🌟 NEW: Main Header (As requested from screenshots) ---
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = "Create new\ntournament",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = DarkTeal,
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fill in the details to create a new padel tournament 🎾",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(end = 40.dp) // Professional right padding
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 🌟 NEW: Tournament Name Input Card ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Section Label
                Text(
                    text = "NAME",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Input Field
                TextField(
                    value = tournamentName,
                    onValueChange = { tournamentName = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BackgroundBeige.copy(alpha = 0.3f),
                        unfocusedContainerColor = BackgroundBeige.copy(alpha = 0.3f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = DarkTeal,
                        unfocusedTextColor = DarkTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 🌟 NEW: Points Per Round Selector Card (image style) ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Section Label
                Text(
                    text = "POINTS PER ROUND",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // The pill selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundBeige.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    availablePoints.forEach { point ->
                        val isSelected = point == selectedPoints

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    color = if (isSelected) White else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) ClayOrange else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedPoints = point },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$point",
                                fontSize = 18.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) DarkTeal else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- EXISTING: Section Label for Players ---
        Text(
            text = "PLAYERS (Manual Entry)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // --- EXISTING: Player Inputs (Team A/B Cards) ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Team A", color = DarkTeal, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = teamAPlayer1, onValueChange = { teamAPlayer1 = it }, label = { Text("P1 Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = teamAPlayer2, onValueChange = { teamAPlayer2 = it }, label = { Text("P2 Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Team B", color = PadelLimeGreen, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = teamBPlayer1, onValueChange = { teamBPlayer1 = it }, label = { Text("P1 Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = teamBPlayer2, onValueChange = { teamBPlayer2 = it }, label = { Text("P2 Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- ACTION BUTTON ---
        Button(
            onClick = {
                // Updated Config to (later) include Tournament details
                onSendToWatch(MatchConfig(teamAPlayer1, teamAPlayer2, teamBPlayer1, teamBPlayer2))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
            shape = CircleShape
        ) {
            Text("Send Tournament to Watch", fontWeight = FontWeight.Bold, color = White, fontSize = 16.sp)
        }
    }
}