package com.example.healthjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.ExerciseCatalogSeeder
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = JournalDatabase.getDatabase(this)
        lifecycleScope.launch {
            ExerciseCatalogSeeder.seed(database.exerciseCatalogDao())
        }
        val journalRepository = JournalRepository(database.journalDao())
        val measurementRepository = com.example.healthjournal.data.BodyMeasurementRepository(
            database.bodyMeasurementDao()
        )
        val viewModelFactory = JournalViewModelFactory(application, journalRepository)
        val measurementViewModelFactory = com.example.healthjournal.viewmodel.BodyMeasurementViewModelFactory(
            measurementRepository
        )
        val goalsRepository = com.example.healthjournal.data.GoalsRepository(database.goalDao())
        val personalCardRepository = com.example.healthjournal.data.PersonalCardRepository(database.personalCardDao())
        val analyticsViewModel = com.example.healthjournal.viewmodel.BodyAnalyticsViewModel(
            measurementRepository,
            goalsRepository
        )
        val fullBackupUseCase = com.example.healthjournal.export.FullBackupUseCase(
            database = database,
            journalRepository = journalRepository,
            bodyMeasurementRepository = measurementRepository,
            goalsRepository = goalsRepository,
            personalCardRepository = personalCardRepository,
            filesDir = application.filesDir,
            exportsDir = java.io.File(application.cacheDir, "exports")
        )
        val exportViewModel = ExportViewModel(application, journalRepository, fullBackupUseCase)
        val restoreViewModel = com.example.healthjournal.export.RestoreViewModel(application)
        val personalCardViewModelFactory = com.example.healthjournal.viewmodel.PersonalCardViewModelFactory(
            personalCardRepository,
            persistUnitSystem = { unitSystem ->
                com.example.healthjournal.data.local.UnitSettings.write(this, unitSystem)
            },
            initialUnitSystem = com.example.healthjournal.data.local.UnitSettings.read(this)
        )
        val workoutRepository = com.example.healthjournal.data.WorkoutRepository(database.workoutSessionDao())
        val workoutHealthSource = com.example.healthjournal.health.HealthConnectWorkoutDataSource(this)

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
                            onExportClick = { navController.navigate("export") },
                            onMeasurementsClick = { navController.navigate("measurements") },
                            onPersonalCardClick = { navController.navigate("personal_card") },
                            onWorkoutClick = { navController.navigate("workout") },
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        com.example.healthjournal.ui.screens.SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("workout") {
                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                        val vibrator = androidx.compose.runtime.remember {
                            getSystemService(android.content.Context.VIBRATOR_SERVICE)
                                as? android.os.Vibrator
                        }
                        val workoutViewModelFactory = androidx.compose.runtime.remember {
                            com.example.healthjournal.viewmodel.WorkoutViewModelFactory(
                                repository = workoutRepository,
                                healthSource = workoutHealthSource,
                                journalRepository = journalRepository,
                                onHaptic = { kind ->
                                    // Routine milestones vibrate via the system
                                    // vibrator so the athlete feels them against
                                    // the bar; session control ticks use the
                                    // strong Compose confirm tick instead.
                                    when (kind) {
                                        com.example.healthjournal.viewmodel.WorkoutHaptic.SET_COMPLETE,
                                        com.example.healthjournal.viewmodel.WorkoutHaptic.EXERCISE_COMPLETE,
                                        com.example.healthjournal.viewmodel.WorkoutHaptic.REST_ENDED -> {
                                            vibrator?.vibrate(
                                                android.os.VibrationEffect.createOneShot(
                                                    if (kind == com.example.healthjournal.viewmodel.WorkoutHaptic.REST_ENDED) 200L else 80L,
                                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                                )
                                            )
                                        }
                                        else -> haptic.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                        )
                                    }
                                },
                                presetRepository = com.example.healthjournal.data.PresetRepository(
                                    database.workoutPresetDao()
                                ),
                                catalogDao = database.exerciseCatalogDao()
                            )
                        }
                        val workoutViewModel: com.example.healthjournal.viewmodel.WorkoutViewModel =
                            viewModel(factory = workoutViewModelFactory)
                        com.example.healthjournal.ui.screens.WorkoutScreen(
                            viewModel = workoutViewModel,
                            onBack = { navController.popBackStack() },
                            onPresetsClick = { navController.navigate("presets") }
                        )
                    }
                    composable("presets") {
                        val presetFactory = androidx.compose.runtime.remember {
                            com.example.healthjournal.viewmodel.PresetViewModelFactory(
                                repository = com.example.healthjournal.data.PresetRepository(
                                    database.workoutPresetDao()
                                ),
                                catalogDao = database.exerciseCatalogDao()
                            )
                        }
                        val presetViewModel: com.example.healthjournal.viewmodel.PresetViewModel =
                            androidx.lifecycle.viewmodel.compose.viewModel(factory = presetFactory)
                        com.example.healthjournal.ui.screens.PresetLibraryScreen(
                            viewModel = presetViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("measurements") {
                        val measurementViewModel: com.example.healthjournal.viewmodel.BodyMeasurementViewModel =
                            viewModel(factory = measurementViewModelFactory)
                        com.example.healthjournal.ui.screens.MeasurementsScreen(
                            viewModel = measurementViewModel,
                            analyticsViewModel = analyticsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("export") {
                        ExportScreen(
                            viewModel = exportViewModel,
                            restoreViewModel = restoreViewModel,
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
                    composable("personal_card") {
                        val personalCardViewModel: com.example.healthjournal.viewmodel.PersonalCardViewModel =
                            viewModel(factory = personalCardViewModelFactory)
                        com.example.healthjournal.ui.screens.PersonalCardScreen(
                            viewModel = personalCardViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
