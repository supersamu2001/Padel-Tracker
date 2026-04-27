package com.example.padeltracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.shared.MatchConfig
import com.example.padeltracker.ui.theme.PadelTrackerTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

// --- COLORS ---
val BackgroundBeige = Color(0xFFEAE3D4)
val DarkTeal = Color(0xFF10363F)
val PadelLimeGreen = Color(0xFFA4C639)
val ClayOrange = Color(0xFFD97D45)
val White = Color(0xFFFFFFFF)

enum class AppScreen { Home, Setup }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PadelTrackerTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.Home) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(BackgroundBeige)
                    ) {
                        when (currentScreen) {
                            AppScreen.Home -> {
                                HomeScreen(
                                    onNewGameClick = { currentScreen = AppScreen.Setup },
                                    onHistoryClick = { /* History functionality */ }
                                )
                            }
                            AppScreen.Setup -> {
                                MatchSetupScreen(
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onSendToWatch = { config -> sendConfigToWatch(config) }
                                )
                            }
                        }
                    }
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
                Toast.makeText(this, "Sent to watch!", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun HomeScreen(onNewGameClick: () -> Unit, onHistoryClick: () -> Unit) {
    // Animation for the bouncing ball
    val infiniteTransition = rememberInfiniteTransition(label = "ball_anim")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f, // Bounces up by 60dp
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TOP SECTION ---
        Spacer(modifier = Modifier.height(20.dp))

        // Rackets Image
        Image(
            painter = painterResource(id = R.drawable.racket_home),
            contentDescription = "Padel Logo",
            modifier = Modifier.size(160.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome back!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DarkTeal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Track your shots and dominate.",
            fontSize = 14.sp,
            color = DarkTeal.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- SMARTWATCH STATUS (Smaller & Directly below text) ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(White, shape = RoundedCornerShape(50.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp) // Much smaller padding
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp) // Very small green dot
                    .background(PadelLimeGreen, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Smartwatch Connected",
                color = DarkTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp // Very small text
            )
        }

        // --- MIDDLE SECTION: Bouncing Ball Area ---
        // This box fills the empty space between the smartwatch status and the buttons below
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter // Aligns the ball just above the buttons
        ) {
            // The actual bouncing ball
            Box(
                modifier = Modifier
                    .offset(y = -offsetY.dp) // Negative value moves it UP
                    .size(24.dp)
                    .background(PadelLimeGreen, shape = CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTTOM SECTION: Buttons ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onNewGameClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Game", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onHistoryClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTeal),
                border = BorderStroke(2.dp, DarkTeal),
                shape = CircleShape
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("History", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(onBackClick: () -> Unit, onSendToWatch: (MatchConfig) -> Unit) {
    var teamAPlayer1 by remember { mutableStateOf("") }
    var teamAPlayer2 by remember { mutableStateOf("") }
    var teamBPlayer1 by remember { mutableStateOf("") }
    var teamBPlayer2 by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("New Match", fontWeight = FontWeight.Bold) },
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

        Spacer(modifier = Modifier.height(16.dp))

        // Cards for Teams
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Team A", color = DarkTeal, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = teamAPlayer1, onValueChange = { teamAPlayer1 = it }, label = { Text("P1") })
                OutlinedTextField(value = teamAPlayer2, onValueChange = { teamAPlayer2 = it }, label = { Text("P2") })
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Team B", color = PadelLimeGreen, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = teamBPlayer1, onValueChange = { teamBPlayer1 = it }, label = { Text("P1") })
                OutlinedTextField(value = teamBPlayer2, onValueChange = { teamBPlayer2 = it }, label = { Text("P2") })
            }
        }

        Button(
            onClick = { onSendToWatch(MatchConfig(teamAPlayer1, teamAPlayer2, teamBPlayer1, teamBPlayer2)) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
            shape = CircleShape
        ) {
            Text("Send to Watch", fontWeight = FontWeight.Bold, color = White)
        }
    }
}