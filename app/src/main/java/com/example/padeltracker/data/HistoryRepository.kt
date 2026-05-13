package com.example.padeltracker.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val matchDao: MatchDao) {

    fun getAllMatches(): Flow<List<MatchRecord>> = matchDao.getAllMatches()

    // Αποθηκεύει έναν νέο αγώνα
    fun insertMatch(match: MatchRecord) {
        matchDao.insertMatch(match)
    }

    // Διαγράφει έναν αγώνα
    suspend fun deleteMatch(match: MatchRecord) {
        matchDao.deleteMatch(match)
    }
}