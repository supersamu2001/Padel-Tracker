package com.example.padeltracker.presentation.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.padeltracker.shared.MatchRules
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.PlayerSetup
import com.example.padeltracker.shared.TeamSetup
import com.example.padeltracker.shared.debug.DebugLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * Save temporary the set-up of the match received from the phone,
 * so that if the app closes accidentally, the set-up setting won't be lost.
 *
 * Bridge between the arrival of setup message from the phone and the actual start of the match.
 * This class ensures robustness and reliability in this phase.
 */
class PendingMatchSetupStore(context: Context) {
    // settings saved through the Android SharedPreferences, that save them in key-value pairs
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Convert the MatchSet object into a JSON string, in order to be saved into the SharedPreferences
    fun save(setup: MatchSetup) {
        val json = setup.toJson().toString()

        prefs.edit {
            putString(KEY_SETUP_JSON, json)
        }

        DebugLogger.d(TAG, "Pending match setup saved: ${setup.matchId}")
    }

    // Retrieve data in memory, reading the JSON string and converting it into a Kotlin object
    fun load(): MatchSetup? {
        val json = prefs.getString(KEY_SETUP_JSON, null) ?: return null

        return try {
            JSONObject(json).toMatchSetup()
        } catch (error: Exception) {
            DebugLogger.e(TAG, "Failed to load pending match setup", error)
            null
        }
    }

    // Read the setup in memory and delete it immediately after
    fun consume(): MatchSetup? {
        val setup = load()
        if (setup != null) {
            clear()
            DebugLogger.d(TAG, "Pending match setup consumed: ${setup.matchId}")
        }
        return setup
    }

    // Remove the data from the memory
    fun clear() {
        prefs.edit {
            remove(KEY_SETUP_JSON)
        }

        DebugLogger.d(TAG, "Pending match setup cleared")
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun isPendingSetupKey(key: String?): Boolean = key == KEY_SETUP_JSON

    private fun MatchSetup.toJson(): JSONObject {
        return JSONObject().apply {
            put("matchId", matchId)
            put("tournamentName", tournamentName)
            put("createdAt", createdAt)
            put("teamA", teamA.toJson())
            put("teamB", teamB.toJson())
            put("rules", rules.toJson())
        }
    }

    private fun TeamSetup.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("players", JSONArray().apply {
                players.forEach { player ->
                    put(player.toJson())
                }
            })
        }
    }

    private fun PlayerSetup.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
        }
    }

    private fun MatchRules.toJson(): JSONObject {
        return JSONObject().apply {
            put("setsToWin", setsToWin)
            put("gamesToWinSet", gamesToWinSet)
            put("tieBreakAt", tieBreakAt)
            put("tieBreakPointsToWin", tieBreakPointsToWin)
            put("minimumAdvantage", minimumAdvantage)
        }
    }

    private fun JSONObject.toMatchSetup(): MatchSetup {
        val defaultRules = MatchRules()

        return MatchSetup(
            matchId = optString("matchId", "unknown_match"),
            tournamentName = optString("tournamentName", ""),
            createdAt = optLong("createdAt", 0L),
            teamA = optJSONObject("teamA")?.toTeamSetup(
                fallbackId = "team_a",
                fallbackName = "Team A"
            ) ?: TeamSetup(
                id = "team_a",
                name = "Team A",
                players = emptyList()
            ),
            teamB = optJSONObject("teamB")?.toTeamSetup(
                fallbackId = "team_b",
                fallbackName = "Team B"
            ) ?: TeamSetup(
                id = "team_b",
                name = "Team B",
                players = emptyList()
            ),
            rules = optJSONObject("rules")?.toMatchRules(defaultRules) ?: defaultRules
        )
    }

    private fun JSONObject.toTeamSetup(
        fallbackId: String,
        fallbackName: String
    ): TeamSetup {
        return TeamSetup(
            id = optString("id", fallbackId),
            name = optString("name", fallbackName),
            players = optJSONArray("players").toPlayerList()
        )
    }

    private fun JSONArray?.toPlayerList(): List<PlayerSetup> {
        if (this == null) return emptyList()

        val players = mutableListOf<PlayerSetup>()

        for (index in 0 until length()) {
            val playerJson = optJSONObject(index) ?: continue

            players.add(
                PlayerSetup(
                    id = playerJson.optString("id", "player_${index + 1}"),
                    name = playerJson.optString("name", "Player ${index + 1}")
                )
            )
        }

        return players
    }

    private fun JSONObject.toMatchRules(defaultRules: MatchRules): MatchRules {
        return MatchRules(
            setsToWin = optInt("setsToWin", defaultRules.setsToWin),
            gamesToWinSet = optInt("gamesToWinSet", defaultRules.gamesToWinSet),
            tieBreakAt = optInt("tieBreakAt", defaultRules.tieBreakAt),
            tieBreakPointsToWin = optInt(
                "tieBreakPointsToWin",
                defaultRules.tieBreakPointsToWin
            ),
            minimumAdvantage = optInt(
                "minimumAdvantage",
                defaultRules.minimumAdvantage
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "pending_match_setup_prefs"
        private const val KEY_SETUP_JSON = "pending_match_setup_json"
        private const val TAG = "PENDING_MATCH_SETUP"
    }
}
