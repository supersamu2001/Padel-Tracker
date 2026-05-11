package com.example.padeltracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
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
import com.example.padeltracker.service.SensorStatusState
import com.example.padeltracker.ui.theme.*
import com.example.padeltracker.ui.components.TennisBallView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    isConnected: Boolean,
    onNewGameClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val lastDataTime by SensorStatusState.lastMessageReceived.collectAsState()
    val accValues by SensorStatusState.lastAccValues.collectAsState()
    val gyroValues by SensorStatusState.lastGyroValues.collectAsState()
    val lastShotTime by SensorStatusState.lastShotTime.collectAsState()
    val lastShotSamples by SensorStatusState.lastShotSamplesCount.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "ball_anim")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y_offset"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // Background Image
        Image(
            painter = painterResource(id = R.drawable.homescreen),
            contentDescription = "Court Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Foreground UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // --- TOP SECTION: Smartwatch connectivity pill ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(50.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isConnected) PadelLimeGreen else Color.Gray,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Smartwatch Connected" else "Searching for Watch...",
                    color = DarkTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // INFO: Sensor data reception status
            lastDataTime?.let { timestamp ->
                val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last data received: $timeString",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "ACC: ${"%.2f".format(accValues[0])}, ${"%.2f".format(accValues[1])}, ${"%.2f".format(accValues[2])}",
                    fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "GYRO: ${"%.2f".format(gyroValues[0])}, ${"%.2f".format(gyroValues[1])}, ${"%.2f".format(gyroValues[2])}",
                    fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Detected Shots Section
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (lastShotTime != null) PadelLimeGreen.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
                ),
                border = BorderStroke(
                    1.dp, 
                    if (lastShotTime != null) PadelLimeGreen else Color.White.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (lastShotTime != null) "SHOT DETECTED" else "NO SHOT DETECTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (lastShotTime != null) {
                        val shotTimeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastShotTime!!))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hour: $shotTimeString",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$lastShotSamples samples",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Text(
                            text = "Swing your racket to record a shot",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Animated bouncing ball area (Pushes the rest to the bottom)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                TennisBallView(
                    modifier = Modifier
                        .offset(y = -offsetY.dp)
                        .size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOTTOM SECTION: Texts just above the buttons ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome back!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 40.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Track every smash, analyze your game, and own the court.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onNewGameClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Game", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = BorderStroke(2.dp, Color.White),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}