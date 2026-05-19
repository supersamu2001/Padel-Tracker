package com.example.padeltracker.data

import kotlinx.coroutines.flow.Flow

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