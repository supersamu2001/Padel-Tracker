package com.example.padeltracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. Create the DataStore instance named "tournament_prefs"
val Context.dataStore by preferencesDataStore(name = "tournament_prefs")

class MatchPreferences(private val context: Context) {

    // 2. Define the Keys to know exactly where each name is saved
    companion object {
        val TEAM_A_P1 = stringPreferencesKey("team_a_p1")
        val TEAM_A_P2 = stringPreferencesKey("team_a_p2")
        val TEAM_B_P1 = stringPreferencesKey("team_b_p1")
        val TEAM_B_P2 = stringPreferencesKey("team_b_p2")
    }

    // 3. Function to SAVE the data (runs asynchronously in the background)
    suspend fun savePlayerNames(a1: String, a2: String, b1: String, b2: String) {
        context.dataStore.edit { preferences ->
            preferences[TEAM_A_P1] = a1
            preferences[TEAM_A_P2] = a2
            preferences[TEAM_B_P1] = b1
            preferences[TEAM_B_P2] = b2
        }
    }

    // 4. Function to READ the data (returns a Flow/Stream of the saved list)
    val playerNamesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        listOf(
            preferences[TEAM_A_P1] ?: "", // If no name is found, return an empty string ""
            preferences[TEAM_A_P2] ?: "",
            preferences[TEAM_B_P1] ?: "",
            preferences[TEAM_B_P2] ?: ""
        )
    }
}