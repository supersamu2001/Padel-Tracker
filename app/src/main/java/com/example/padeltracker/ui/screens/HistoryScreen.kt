package com.example.padeltracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.padeltracker.data.MatchRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    matches: List<MatchRecord>,
    onBackClick: () -> Unit,
    onMatchClick: (MatchRecord) -> Unit
) {
    // Intercept system back button to go back to Home
    BackHandler {
        onBackClick()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            // ΔΙΟΡΘΩΣΗ: Αλλάξαμε το historyscreen_jpg σε historyscreen
            painter = painterResource(id = R.drawable.historyscreen),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Match History", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (matches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matches played yet 🎾", color = Color.White.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(matches) { match ->
                        MatchHistoryCard(match = match, onClick = { onMatchClick(match) })
                    }
                }
            }
        }
    }
}

@Composable
fun MatchHistoryCard(match: MatchRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = match.date, fontSize = 12.sp, color = Color.Gray)
                Text(text = "Final Score", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = match.teamAPlayers, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "vs", fontSize = 12.sp, color = Color.Gray)
                    Text(text = match.teamBPlayers, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(text = match.score, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF008080))
            }
        }
    }
}