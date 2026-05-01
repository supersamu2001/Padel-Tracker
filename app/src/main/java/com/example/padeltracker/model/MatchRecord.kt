package com.example.padeltracker.model

// This is the blueprint for our data.
// Every match in the history will follow this structure.
data class MatchRecord(
    val date: String,
    val duration: String,
    val score: String,
    val avgHeartRate: Int,
    val forehands: Int,
    val backhands: Int,
    val forehandLobs : Int,
    val backhandLobs: Int,
    val smashes: Int,
    val services: Int
)