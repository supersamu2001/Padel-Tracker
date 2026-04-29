package com.example.padeltracker.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.padeltracker.presentation.model.*
import com.example.padeltracker.presentation.theme.PadelTrackerTheme
import com.example.padeltracker.presentation.viewmodel.MatchViewModel

@Composable
fun WearApp(viewModel: MatchViewModel) {
    val state = viewModel.state.value
    
    PadelTrackerTheme {
        AppScaffold {
            when (state.currentMatch.status) {
                MatchStatus.NOT_STARTED -> StartMatchScreen(state, onStart = { viewModel.startMatch() })
                MatchStatus.SELECTING_SERVER -> SelectServerScreen(onSelect = { viewModel.selectInitialServer(it) })
                MatchStatus.IN_PROGRESS -> MatchScoreScreen(
                    state = state,
                    onAddPoint = { viewModel.addPoint(it) },
                    onUndo = { viewModel.undo() }
                )
                MatchStatus.FINISHED -> MatchFinishedScreen(
                    state = state,
                    onReset = { viewModel.resetMatch() },
                    onUndo = { viewModel.undo() }
                )
            }
        }
    }
}

@Composable
fun StartMatchScreen(state: ScoreTrackerState, onStart: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onStart) {
                Text("Start Match")
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(text = "Padel Tracker", textAlign = TextAlign.Center)
                }
            }
            item {
                TeamInfo(state.currentMatch.teamA)
            }
            item {
                TeamInfo(state.currentMatch.teamB)
            }
        }
    }
}

@Composable
fun TeamInfo(team: Team) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = team.name, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        Text(
            text = team.players.joinToString(" & ") { it.name },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SelectServerScreen(onSelect: (TeamId) -> Unit) {
    ScreenScaffold { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Who serves?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSelect(TeamId.TEAM_A) }) {
                    Text("Team A")
                }
                Button(onClick = { onSelect(TeamId.TEAM_B) }) {
                    Text("Team B")
                }
            }
        }
    }
}

@Composable
fun MatchScoreScreen(
    state: ScoreTrackerState,
    onAddPoint: (TeamId) -> Unit,
    onUndo: () -> Unit
) {
    val match = state.currentMatch
    val teamASets = match.completedSets.count { it.teamAGames > it.teamBGames }
    val teamBSets = match.completedSets.count { it.teamBGames > it.teamAGames }

    ScreenScaffold { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Set Score Header
            Text(
                text = "Sets: $teamASets - $teamBSets",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Games: ${match.currentSet.teamAGames} - ${match.currentSet.teamBGames}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Points Area
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreButton(
                    label = "Team A",
                    points = formatPointDisplay(
                        match.currentSet.currentGame.teamAPoints,
                        match.currentSet.currentGame.teamBPoints,
                        match.currentSet.currentGame.type
                    ),
                    isServing = match.servingTeam == TeamId.TEAM_A,
                    onClick = { onAddPoint(TeamId.TEAM_A) }
                )
                ScoreButton(
                    label = "Team B",
                    points = formatPointDisplay(
                        match.currentSet.currentGame.teamBPoints,
                        match.currentSet.currentGame.teamAPoints,
                        match.currentSet.currentGame.type
                    ),
                    isServing = match.servingTeam == TeamId.TEAM_B,
                    onClick = { onAddPoint(TeamId.TEAM_B) }
                )
            }

            // Controls
            IconButton(onClick = onUndo) {
                Text("Undo", fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ScoreButton(label: String, points: String, isServing: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Button(
            onClick = onClick,
            modifier = Modifier.size(60.dp),
            colors = if (isServing) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
        ) {
            Text(text = points, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        if (isServing) {
            Text(text = "SERVING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        } else {
            Spacer(modifier = Modifier.height(14.dp)) // Maintain alignment
        }
    }
}

@Composable
fun MatchFinishedScreen(state: ScoreTrackerState, onReset: () -> Unit, onUndo: () -> Unit) {
    val match = state.currentMatch
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onReset) {
                Text("New Match")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(text = "MATCH FINISHED", textAlign = TextAlign.Center)
                }
            }
            item {
                Text(
                    text = "${if (match.winner == TeamId.TEAM_A) "Team A" else "Team B"} Wins!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            item {
                Text(text = "Final Score:", style = MaterialTheme.typography.labelSmall)
            }
            item {
                Text(
                    text = match.completedSets.joinToString(" | ") { "${it.teamAGames}-${it.teamBGames}" },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                TextButton(onClick = onUndo) {
                    Text("Undo Final Point")
                }
            }
        }
    }
}

/**
 * Formats numeric points into padel display strings (0, 15, 30, 40, AD).
 * Used for UI display only.
 */
private fun formatPointDisplay(points: Int, opponentPoints: Int, type: GameType): String {
    if (type == GameType.TIE_BREAK) return points.toString()
    
    return when (points) {
        0 -> "0"
        1 -> "15"
        2 -> "30"
        3 -> "40"
        else -> {
            if (points > opponentPoints) "AD"
            else "40"
        }
    }
}
