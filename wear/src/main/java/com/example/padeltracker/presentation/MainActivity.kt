package com.example.padeltracker.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.padeltracker.presentation.theme.PadelTrackerTheme
import com.example.padeltracker.shared.MatchConfig
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private var matchConfig by mutableStateOf(MatchConfig())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(matchConfig)
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        fetchConfig()
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    private fun fetchConfig() {
        Wearable.getDataClient(this).dataItems
            .addOnSuccessListener { dataItemBuffer ->
                dataItemBuffer.forEach { item ->
                    if (item.uri.path == MatchConfig.PATH) {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        updateConfig(dataMap)
                    }
                }
                dataItemBuffer.release()
            }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == MatchConfig.PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                updateConfig(dataMap)
                Log.d("PadelTracker", "Config received via event: $matchConfig")
            }
        }
    }

    private fun updateConfig(dataMap: com.google.android.gms.wearable.DataMap) {
        matchConfig = MatchConfig(
            teamAPlayer1 = dataMap.getString(MatchConfig.KEY_TEAM_A_P1, "Player 1"),
            teamAPlayer2 = dataMap.getString(MatchConfig.KEY_TEAM_A_P2, "Player 2"),
            teamBPlayer1 = dataMap.getString(MatchConfig.KEY_TEAM_B_P1, "Player 1"),
            teamBPlayer2 = dataMap.getString(MatchConfig.KEY_TEAM_B_P2, "Player 2")
        )
    }
}

@Composable
fun WearApp(config: MatchConfig) {
    PadelTrackerTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()
            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    EdgeButton(
                        onClick = { /* Start Match */ },
                    ) {
                        Text("Start Match")
                    }
                },
            ) { contentPadding ->
                TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                    item {
                        ListHeader(
                            modifier =
                                Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(text = "Match Setup", textAlign = TextAlign.Center)
                        }
                    }
                    item {
                        PlayerInfo("Team A", config.teamAPlayer1, config.teamAPlayer2)
                    }
                    item {
                        PlayerInfo("Team B", config.teamBPlayer1, config.teamBPlayer2)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerInfo(teamName: String, p1: String, p2: String) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(text = teamName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        Text(text = "$p1 & $p2", textAlign = TextAlign.Center)
    }
}
