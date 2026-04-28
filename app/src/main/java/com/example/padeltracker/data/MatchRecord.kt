package com.example.padeltracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// This class represents a single match in our database table
@Entity(tableName = "matches")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,          // Example: "28/04/2026"
    val duration: String,      // Example: "1h 15m"
    val score: String,         // Example: "6-4, 6-2"
    val avgHeartRate: Int,     // Average BPM from Watch
    val forehands: Int,        // Total forehand shots
    val backhands: Int,        // Total backhand shots
    val teamAPlayers: String,  // Names of Team A
    val teamBPlayers: String   // Names of Team B
)