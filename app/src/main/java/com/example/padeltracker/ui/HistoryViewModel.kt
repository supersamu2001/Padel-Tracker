package com.example.padeltracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padeltracker.data.HistoryRepository
import com.example.padeltracker.data.MatchRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {

    // Λίστα αγώνων που ανανεώνεται αυτόματα
    val matches: StateFlow<List<MatchRecord>> = repository
        .getAllMatches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Διαγραφή αγώνα
    fun deleteMatch(match: MatchRecord) {
        viewModelScope.launch {
            repository.deleteMatch(match)
        }
    }
    fun saveMatch(match: MatchRecord) {
        viewModelScope.launch {
            repository.insertMatch(match)
        }
    }
}