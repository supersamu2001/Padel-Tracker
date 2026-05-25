package com.example.padeltracker.data

import kotlinx.coroutines.flow.Flow

// Bridge between MatchDao and MainActivity, in order to avoid that UI communicates directly with the database
class HistoryRepository(private val matchDao: MatchDao) {

    fun getAllMatches(): Flow<List<MatchRecord>> = matchDao.getAllMatches()

    // save new game
    suspend fun insertMatch(match: MatchRecord) {
        matchDao.insertMatch(match)
    }

    // delete a game
    suspend fun deleteMatch(match: MatchRecord) {
        matchDao.deleteMatch(match)
    }
}

// suspend function: can be paused if there are other operations to do.
// Allows the user interface to work properly even though I'm running heavy functions