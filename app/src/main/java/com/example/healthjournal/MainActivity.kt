package com.example.healthjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.ui.screens.AddEntryScreen
import com.example.healthjournal.ui.screens.ArchiveScreen
import com.example.healthjournal.ui.screens.HistoryScreen
import com.example.healthjournal.ui.screens.ComponentPreviewScreen
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.viewmodel.JournalViewModel
import com.example.healthjournal.viewmodel.JournalViewModelFactory
import com.example.healthjournal.sync.SyncManager
import com.example.healthjournal.export.ExportViewModel
import com.example.healthjournal.ui.screens.ExportScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = JournalDatabase.getDatabase(this)
        val journalRepository = JournalRepository(database.journalDao())
        val measurementRepository = com.example.healthjournal.data.BodyMeasurementRepository(
            database.bodyMeasurementDao()
        )
        val viewModelFactory = JournalViewModelFactory(application, journalRepository)
        val measurementViewModelFactory = com.example.healthjournal.viewmodel.BodyMeasurementViewModelFactory(
            measurementRepository
        )
        val exportViewModel = ExportViewModel(application, journalRepository)

        // Trigger sync on start
        SyncManager.enqueuePeriodicSync(this)

        setContent {
            HealthJournalTheme {
                val navController = rememberNavController()
                val viewModel: JournalViewModel = viewModel(factory = viewModelFactory)

                NavHost(navController = navController, startDestination = "history") {
                    composable("history") {
                        HistoryScreen(
                            viewModel = viewModel,
                            measurementViewModelFactory = measurementViewModelFactory,
                            onAddEntryClick = { navController.navigate("add_entry") },
                            onEntryClick = { entryId -> navController.navigate("add_entry?entryId=$entryId") },
                            onArchiveClick = { navController.navigate("archive") },
                            onExportClick = { navController.navigate("export") }
                        )
                    }
                    composable("export") {
                        ExportScreen(
                            viewModel = exportViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("archive") {
                        ArchiveScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onEntryClick = { entryId -> navController.navigate("add_entry?entryId=$entryId") }
                        )
                    }
                    composable(
                        route = "add_entry?entryId={entryId}",
                        arguments = listOf(
                            androidx.navigation.navArgument("entryId") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val entryId = backStackEntry.arguments?.getString("entryId")
                        AddEntryScreen(
                            viewModel = viewModel,
                            entryId = entryId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("component_preview") {
                        ComponentPreviewScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
