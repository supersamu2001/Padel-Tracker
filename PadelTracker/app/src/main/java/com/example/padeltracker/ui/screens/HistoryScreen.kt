package com.example.padeltracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.model.MatchRecord
import com.example.padeltracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBackClick: () -> Unit) {
    val mockHistory = listOf(
        MatchRecord("Today, 18:30", "1h 15m", "6-4, 7-5 (Won)", 142, 45, 32),
        MatchRecord("Yesterday, 19:00", "55m", "4-6, 3-6 (Lost)", 135, 28, 41),
        MatchRecord("April 24, 10:00", "1h 30m", "6-2, 4-6, 6-3 (Won)", 150, 52, 38)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Match History", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBackClick) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = ClayOrange) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundBeige, titleContentColor = DarkTeal)
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            items(mockHistory) { match ->
                MatchHistoryCard(match)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MatchHistoryCard(match: MatchRecord) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = match.date, fontSize = 12.sp, color = Color.Gray)
            Text(text = match.score, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DarkTeal)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BackgroundBeige)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ClayOrange, modifier = Modifier.size(16.dp))
                    Text(text = match.duration, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Text(text = "${match.avgHeartRate} bpm", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.background(BackgroundBeige, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(text = "Forehands: ${match.forehands}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
                }
                Box(modifier = Modifier.background(PadelLimeGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(text = "Backhands: ${match.backhands}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
                }
            }
        }
    }
}