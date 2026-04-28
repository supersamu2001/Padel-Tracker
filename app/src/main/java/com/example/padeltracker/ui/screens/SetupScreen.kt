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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.data.MatchPreferences
import com.example.padeltracker.shared.MatchConfig
import com.example.padeltracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(onBackClick: () -> Unit, onSendToWatch: (MatchConfig) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { MatchPreferences(context) }

    // States for inputs
    var tournamentName by remember { mutableStateOf("") }
    var selectedPoints by remember { mutableStateOf(8) }
    val pointsOptions = listOf(8, 16, 21, 24, 32, 40)

    var tAP1 by remember { mutableStateOf("") }
    var tAP2 by remember { mutableStateOf("") }
    var tBP1 by remember { mutableStateOf("") }
    var tBP2 by remember { mutableStateOf("") }

    // 🌟 1. READ DATA: Load saved names when the screen opens
    LaunchedEffect(Unit) {
        prefs.playerNamesFlow.collect { savedNames ->
            if (savedNames[0].isNotEmpty()) tAP1 = savedNames[0]
            if (savedNames[1].isNotEmpty()) tAP2 = savedNames[1]
            if (savedNames[2].isNotEmpty()) tBP1 = savedNames[2]
            if (savedNames[3].isNotEmpty()) tBP2 = savedNames[3]
        }
    }

    val isFormValid = tournamentName.isNotBlank() &&
            tAP1.isNotBlank() && tAP2.isNotBlank() &&
            tBP1.isNotBlank() && tBP2.isNotBlank()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .background(BackgroundBeige)
    ) {
        TopAppBar(
            title = { Text("") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = ClayOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundBeige)
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Create new\ntournament", fontSize = 36.sp, fontWeight = FontWeight.Black, color = DarkTeal, lineHeight = 40.sp)
            Text("Fill in the details to create a new padel tournament 🎾", color = Color.Gray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // Tournament Name
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NAME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    OutlinedTextField(
                        value = tournamentName,
                        onValueChange = { tournamentName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Monday tournament", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Points Selector
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("POINTS PER ROUND", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().background(BackgroundBeige.copy(0.5f), RoundedCornerShape(12.dp)).padding(4.dp)) {
                        pointsOptions.forEach { pt ->
                            Box(
                                modifier = Modifier.weight(1f).height(40.dp)
                                    .background(if (selectedPoints == pt) White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .border(if (selectedPoints == pt) 2.dp else 0.dp, if (selectedPoints == pt) ClayOrange else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedPoints = pt },
                                contentAlignment = Alignment.Center
                            ) { Text("$pt", fontWeight = FontWeight.Bold, color = if (selectedPoints == pt) DarkTeal else Color.Gray) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("PLAYERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            // Team A
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Team A", color = DarkTeal, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = tAP1, onValueChange = { tAP1 = it }, label = { Text("Player 1 Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
                    OutlinedTextField(value = tAP2, onValueChange = { tAP2 = it }, label = { Text("Player 2 Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
                }
            }

            // Team B
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Team B", color = PadelLimeGreen, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = tBP1, onValueChange = { tBP1 = it }, label = { Text("Player 1 Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
                    OutlinedTextField(value = tBP2, onValueChange = { tBP2 = it }, label = { Text("Player 2 Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                enabled = isFormValid,
                onClick = {
                    // 🌟 2. SAVE DATA: Save names to storage before moving forward
                    scope.launch {
                        prefs.savePlayerNames(tAP1, tAP2, tBP1, tBP2)
                    }
                    onSendToWatch(MatchConfig(tAP1, tAP2, tBP1, tBP2))
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