package com.example.padeltracker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.padeltracker.presentation.ui.WearApp
import com.example.padeltracker.presentation.viewmodel.MatchViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val matchViewModel: MatchViewModel = viewModel()
            WearApp(matchViewModel)
        }
    }
}
