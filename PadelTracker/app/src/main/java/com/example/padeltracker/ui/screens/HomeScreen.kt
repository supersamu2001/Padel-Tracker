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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padeltracker.R
import com.example.padeltracker.ui.theme.*
import com.example.padeltracker.ui.components.TennisBallView

@Composable
fun HomeScreen(
    isConnected: Boolean, // New parameter to receive connection status
    onNewGameClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    // Animation for the bouncing tennis ball
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Image(
            painter = painterResource(id = R.drawable.racket_home),
            contentDescription = "App Logo",
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome back!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DarkTeal
        )

        Spacer(modifier = Modifier.height(12.dp))

        // DYNAMIC UI: Smartwatch connectivity pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(White, shape = RoundedCornerShape(50.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Status Dot: Changes color based on 'isConnected' state
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
                // Text updates based on connectivity
                text = if (isConnected) "Smartwatch Connected" else "Searching for Watch...",
                color = DarkTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Animated ball area
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
                Icon(Icons.Default.Add, contentDescription = null, tint = White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Game", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onHistoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = BorderStroke(2.dp, DarkTeal),
                shape = CircleShape
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = DarkTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
            }
        }
    }
}