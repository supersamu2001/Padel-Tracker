package com.example.padeltracker.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.shared.MatchConfig
import com.example.padeltracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(onBackClick: () -> Unit, onSendToWatch: (MatchConfig) -> Unit) {
    var teamAPlayer1 by remember { mutableStateOf("") }
    var teamAPlayer2 by remember { mutableStateOf("") }
    var teamBPlayer1 by remember { mutableStateOf("") }
    var teamBPlayer2 by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(
            title = { Text("New Match", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ClayOrange) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundBeige, titleContentColor = DarkTeal)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Team A", color = DarkTeal, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = teamAPlayer1, onValueChange = { teamAPlayer1 = it }, label = { Text("P1") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = teamAPlayer2, onValueChange = { teamAPlayer2 = it }, label = { Text("P2") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Team B", color = PadelLimeGreen, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = teamBPlayer1, onValueChange = { teamBPlayer1 = it }, label = { Text("P1") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = teamBPlayer2, onValueChange = { teamBPlayer2 = it }, label = { Text("P2") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Button(
            onClick = { onSendToWatch(MatchConfig(teamAPlayer1, teamAPlayer2, teamBPlayer1, teamBPlayer2)) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
            shape = CircleShape
        ) {
            Text("Send to Watch", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}