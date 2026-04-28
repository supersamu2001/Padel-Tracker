package com.example.padeltracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    // ΒΓΑΛΑΜΕ ΤΗ ΛΕΞΗ suspend ΕΝΤΕΛΩΣ!
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMatch(match: MatchRecord)

    @Query("SELECT * FROM matches ORDER BY id DESC")
    fun getAllMatches(): Flow<List<MatchRecord>>
}