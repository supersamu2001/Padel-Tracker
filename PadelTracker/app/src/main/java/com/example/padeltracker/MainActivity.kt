package com.example.padeltracker

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.padeltracker.shared.MatchConfig
import com.example.padeltracker.ui.theme.PadelTrackerTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PadelTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MatchSetupScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSendToWatch = { config ->
                            sendConfigToWatch(config)
                        }
                    )
                }
            }
        }
    }

    private fun sendConfigToWatch(config: MatchConfig) {
        val putDataMapReq = PutDataMapRequest.create(MatchConfig.PATH)
        putDataMapReq.dataMap.putString(MatchConfig.KEY_TEAM_A_P1, config.teamAPlayer1)
        putDataMapReq.dataMap.putString(MatchConfig.KEY_TEAM_A_P2, config.teamAPlayer2)
        putDataMapReq.dataMap.putString(MatchConfig.KEY_TEAM_B_P1, config.teamBPlayer1)
        putDataMapReq.dataMap.putString(MatchConfig.KEY_TEAM_B_P2, config.teamBPlayer2)
        putDataMapReq.setUrgent()

        val putDataReq = putDataMapReq.asPutDataRequest()
        Wearable.getDataClient(this).putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d("PadelTracker", "Configuration sent successfully")
                Toast.makeText(this, "Configuration sent!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("PadelTracker", "Failed to send data", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        
        // Check for connected nodes (informational)
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w("PadelTracker", "No connected nodes found!")
                    Toast.makeText(this, "No watch connected!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("PadelTracker", "Found ${nodes.size} connected nodes")
                }
            }
    }
}

@Composable
fun MatchSetupScreen(modifier: Modifier = Modifier, onSendToWatch: (MatchConfig) -> Unit) {
    var taP1 by remember { mutableStateOf("Player 1") }
    var taP2 by remember { mutableStateOf("Player 2") }
    var tbP1 by remember { mutableStateOf("Player 1") }
    var tbP2 by remember { mutableStateOf("Player 2") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Team A", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = taP1,
            onValueChange = { taP1 = it },
            label = { Text("Player 1") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = taP2,
            onValueChange = { taP2 = it },
            label = { Text("Player 2") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Team B", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = tbP1,
            onValueChange = { tbP1 = it },
            label = { Text("Player 1") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = tbP2,
            onValueChange = { tbP2 = it },
            label = { Text("Player 2") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSendToWatch(MatchConfig(taP1, taP2, tbP1, tbP2)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send to Watch")
        }
    }
}
