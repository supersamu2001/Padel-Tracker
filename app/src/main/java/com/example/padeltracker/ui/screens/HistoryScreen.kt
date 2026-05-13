package com.example.padeltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.data.AppDatabase
import com.example.padeltracker.data.MatchRecord
import com.example.padeltracker.ui.theme.BackgroundBeige
import com.example.padeltracker.ui.theme.ClayOrange
import com.example.padeltracker.ui.theme.DarkTeal
import com.example.padeltracker.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    // Initialize Database
    val db = remember { AppDatabase.getDatabase(context) }

    // Collect the Flow from Room as a Compose State
    val matches by db.matchDao().getAllMatches().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match History", fontWeight = FontWeight.Bold, color = DarkTeal) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = ClayOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundBeige)
            )
        },
        containerColor = BackgroundBeige
    ) { padding ->
        if (matches.isEmpty()) {
            // Displayed if the database is empty
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No matches recorded yet 🎾", color = Color.Gray)
            }
        } else {
            // Display the list of matches
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(matches) { match ->
                    MatchHistoryCard(match)
                }
            }
        }
    }
}

@Composable
fun MatchHistoryCard(match: MatchRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date and Final Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = match.date, fontSize = 12.sp, color = Color.Gray)
                Text(
                    text = match.score,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = ClayOrange
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = BackgroundBeige)

            // Players Info & Winner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.teamAPlayers} vs ${match.teamBPlayers}",
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    fontSize = 14.sp
                )
                // ΕΔΩ ΧΡΗΣΙΜΟΠΟΙΟΥΜΕ ΤΟ WINNER ΠΟΥ ΕΒΑΛΕΣ ΣΤΗ ΒΑΣΗ!
                Text(
                    text = "🏆 ${match.winner}",
                    fontWeight = FontWeight.Bold,
                    color = ClayOrange,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row 1 (Χωρισμένα για να χωράνε στην οθόνη)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatItem(label = "BPM", value = "${match.avgHeartRate}")
                StatItem(label = "Smashes", value = "${match.smashes}")
                StatItem(label = "Services", value = "${match.services}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats Row 2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatItem(label = "FH", value = "${match.forehands}")
                StatItem(label = "BH", value = "${match.backhands}")
                StatItem(label = "FH Lobs", value = "${match.forehandLobs}")
                StatItem(label = "BH Lobs", value = "${match.backhandLobs}")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
    }
}