package com.example.padeltracker.presentation.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.padeltracker.shared.MatchRules
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.PlayerSetup
import com.example.padeltracker.shared.TeamSetup
import org.json.JSONArray
import org.json.JSONObject

class PendingMatchSetupStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun save(setup: MatchSetup) {
        val json = setup.toJson().toString()

        prefs.edit()
            .putString(KEY_SETUP_JSON, json)
            .apply()

        Log.d(TAG, "Pending match setup saved: ${setup.matchId}")
    }

    fun load(): MatchSetup? {
        val json = prefs.getString(KEY_SETUP_JSON, null) ?: return null

        return try {
            JSONObject(json).toMatchSetup()
        } catch (error: Exception) {
            Log.e(TAG, "Failed to load pending match setup", error)
            null
        }
    }

    fun consume(): MatchSetup? {
        val setup = load()
        if (setup != null) {
            clear()
            Log.d(TAG, "Pending match setup consumed: ${setup.matchId}")
        }
        return setup
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_SETUP_JSON)
            .apply()

        Log.d(TAG, "Pending match setup cleared")
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
