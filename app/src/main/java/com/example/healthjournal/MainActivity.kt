package com.example.healthjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.ui.screens.AddEntryScreen
import com.example.healthjournal.ui.screens.HistoryScreen
import com.example.healthjournal.ui.screens.ComponentPreviewScreen
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.viewmodel.JournalViewModel
import com.example.healthjournal.viewmodel.JournalViewModelFactory
import com.example.healthjournal.sync.SyncManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = JournalDatabase.getDatabase(this)
        val repository = JournalRepository(database.journalDao())
        val viewModelFactory = JournalViewModelFactory(application, repository)

        // Trigger sync on start
        SyncManager.enqueueSync(this)

        setContent {
            HealthJournalTheme {
                val navController = rememberNavController()
                val viewModel: JournalViewModel = viewModel(factory = viewModelFactory)

                NavHost(navController = navController, startDestination = "component_preview") {
                    composable("history") {
                        HistoryScreen(
                            viewModel = viewModel,
                            onAddEntryClick = { navController.navigate("add_entry") }
                        )
                    }
                    composable("add_entry") {
                        AddEntryScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("component_preview") {
                        ComponentPreviewScreen(
                            onBack = { navController.navigate("history") }
                        )
                    }
                }
            }
        }
    }
}
