package com.example.padeltracker.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

// Shared style constants
private val TeamAColor = Color(0xFF00BCD4) // Cyan/Blue
private val TeamBColor = Color(0xFFE91E63) // Magenta/Purple
private const val PanelAlpha = 0.05f
private const val BorderAlpha = 0.1f

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
                    onUndo = { viewModel.undo() },
                    onEndMatch = { viewModel.endMatchEarly() }
                )
                MatchStatus.FINISHED -> MatchFinishedScreen(
                    state = state,
                    onEndMatch = { viewModel.confirmEndMatch() },
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
                CompactTeamCard(state.currentMatch.teamA, TeamAColor)
            }
            item {
                CompactTeamCard(state.currentMatch.teamB, TeamBColor)
            }
        }
    }
}

@Composable
fun CompactTeamCard(team: Team, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(color.copy(alpha = PanelAlpha), shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = team.name, 
            color = color, 
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = team.players.joinToString(" & ") { it.name },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
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
                text = "Who serves first?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ServerSelectionCard(
                    modifier = Modifier.weight(1f),
                    label = "Team A",
                    color = TeamAColor,
                    onClick = { onSelect(TeamId.TEAM_A) }
                )
                ServerSelectionCard(
                    modifier = Modifier.weight(1f),
                    label = "Team B",
                    color = TeamBColor,
                    onClick = { onSelect(TeamId.TEAM_B) }
                )
            }
        }
    }
}

@Composable
fun ServerSelectionCard(modifier: Modifier, label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(60.dp)
            .background(color.copy(alpha = PanelAlpha), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScoreHeader(
    teamASets: Int,
    teamBSets: Int,
    teamAGames: Int,
    teamBGames: Int,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            onClick = onMenuClick,
            modifier = Modifier
                .height(20.dp)
                .width(36.dp)
        ) {
            Text(
                text = "⋮",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "SETS: $teamASets - $teamBSets",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            fontSize = 11.sp
        )

        Text(
            text = "GAMES: $teamAGames - $teamBGames",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun TeamScoreArea(
    modifier: Modifier = Modifier,
    label: String,
    points: String,
    isServing: Boolean,
    servingPlayer: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(1.dp)
            .background(color.copy(alpha = 0.05f), shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = points,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = color
            )

            if (isServing) {
                Text(
                    text = "SERVING",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = servingPlayer,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun UndoArea(onUndo: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(
                Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
            )
            .combinedClickable(
                onClick = { /* Normal tap ignored */ },
                onLongClick = onUndo
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Hold to undo",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MatchScoreScreen(
    state: ScoreTrackerState,
    onAddPoint: (TeamId) -> Unit,
    onUndo: () -> Unit,
    onEndMatch: () -> Unit
) {
    val match = state.currentMatch
    val teamASets = match.completedSets.count { it.teamAGames > it.teamBGames }
    val teamBSets = match.completedSets.count { it.teamBGames > it.teamAGames }

    val currentServingPlayerName = when (match.servingTeam) {
        TeamId.TEAM_A -> match.teamA.players.getOrNull(match.servingPlayerIndex ?: 0)?.name ?: "Player"
        TeamId.TEAM_B -> match.teamB.players.getOrNull(match.servingPlayerIndex ?: 0)?.name ?: "Player"
        else -> "Player"
    }

    var showEndMatchAction by remember { mutableStateOf(false) }

    ScreenScaffold { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Compact Header with menu trigger
                ScoreHeader(
                    teamASets = teamASets,
                    teamBSets = teamBSets,
                    teamAGames = match.currentSet.teamAGames,
                    teamBGames = match.currentSet.teamBGames,
                    onMenuClick = { showEndMatchAction = true }
                )

                // 2. Main Score Area (Tappable Zones)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Team A Zone (Left)
                        TeamScoreArea(
                            modifier = Modifier.weight(1f),
                            label = "Team A",
                            points = formatPointDisplay(
                                match.currentSet.currentGame.teamAPoints,
                                match.currentSet.currentGame.teamBPoints,
                                match.currentSet.currentGame.type
                            ),
                            isServing = match.servingTeam == TeamId.TEAM_A,
                            servingPlayer = currentServingPlayerName,
                            color = TeamAColor,
                            onClick = { onAddPoint(TeamId.TEAM_A) }
                        )

                        // Vertical Separator
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Color.White.copy(alpha = BorderAlpha))
                        )

                        // Team B Zone (Right)
                        TeamScoreArea(
                            modifier = Modifier.weight(1f),
                            label = "Team B",
                            points = formatPointDisplay(
                                match.currentSet.currentGame.teamBPoints,
                                match.currentSet.currentGame.teamAPoints,
                                match.currentSet.currentGame.type
                            ),
                            isServing = match.servingTeam == TeamId.TEAM_B,
                            servingPlayer = currentServingPlayerName,
                            color = TeamBColor,
                            onClick = { onAddPoint(TeamId.TEAM_B) }
                        )
                    }
                }

                // Horizontal Separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = BorderAlpha))
                )

                // 3. Bottom Undo Area
                UndoArea(onUndo = onUndo)
            }

            // End Match Confirmation Overlay
            if (showEndMatchAction) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable { showEndMatchAction = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .background(Color.DarkGray, shape = RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Court time over?",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(onClick = {
                                    showEndMatchAction = false
                                    onEndMatch()
                                })
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "End match",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchFinishedScreen(state: ScoreTrackerState, onEndMatch: () -> Unit, onUndo: () -> Unit) {
    val match = state.currentMatch

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onEndMatch) {
                Text("End Match")
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    val headerText = if (match.endedEarly) {
                        "MATCH STOPPED"
                    } else {
                        "MATCH FINISHED"
                    }

                    Text(text = headerText, textAlign = TextAlign.Center)
                }
            }

            item {
                if (match.endedEarly) {
                    Text(
                        text = "Draw",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val winnerColor = if (match.winner == TeamId.TEAM_A) {
                        TeamAColor
                    } else {
                        TeamBColor
                    }

                    val winnerName = if (match.winner == TeamId.TEAM_A) {
                        "Team A"
                    } else {
                        "Team B"
                    }

                    Text(
                        text = "$winnerName Wins!",
                        style = MaterialTheme.typography.titleMedium,
                        color = winnerColor,
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Text(
                    text = "Final Score:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            item {
                val completedScore = match.completedSets.joinToString(" | ") {
                    "${it.teamAGames}-${it.teamBGames}"
                }

                val currentSetScore = "Current: ${match.currentSet.teamAGames}-${match.currentSet.teamBGames}"

                val finalScore = if (match.endedEarly) {
                    if (match.completedSets.isEmpty()) {
                        currentSetScore
                    } else {
                        "$completedScore | $currentSetScore"
                    }
                } else {
                    completedScore
                }

                Text(
                    text = finalScore,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            item {
                TextButton(
                    onClick = onUndo,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Undo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
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
