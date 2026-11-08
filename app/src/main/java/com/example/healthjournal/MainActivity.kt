package com.example.healthjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.RoomProfileDataEraser
import com.example.healthjournal.data.SharedPreferencesActiveProfileStore
import com.example.healthjournal.data.local.ExerciseCatalogSeeder
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.ui.format.AndroidWarmupHistoryFormatter
import com.example.healthjournal.ui.screens.AddEntryScreen
import com.example.healthjournal.ui.screens.ArchiveScreen
import com.example.healthjournal.ui.screens.HistoryScreen
import com.example.healthjournal.ui.screens.MainScreen
import com.example.healthjournal.ui.screens.ManageProfilesScreen
import com.example.healthjournal.ui.components.ProfileSwitcher
import com.example.healthjournal.ui.screens.ComponentPreviewScreen
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.viewmodel.JournalViewModel
import com.example.healthjournal.viewmodel.JournalViewModelFactory
import com.example.healthjournal.viewmodel.ProfileViewModel
import com.example.healthjournal.viewmodel.ProfileViewModelFactory
import com.example.healthjournal.sync.SyncManager
import com.example.healthjournal.export.ExportViewModel
import com.example.healthjournal.ui.screens.ExportScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = JournalDatabase.getDatabase(this)
        val profileRepository = ProfileRepository(
            profileDao = database.profileDao(),
            settings = SharedPreferencesActiveProfileStore(this),
            eraser = RoomProfileDataEraser(database, application.filesDir)
        )
        lifecycleScope.launch {
            profileRepository.ensureSelfProfile()
            ExerciseCatalogSeeder.seed(database.exerciseCatalogDao())
        }
        val journalRepository = JournalRepository(database.journalDao(), profileRepository)
        val measurementRepository = com.example.healthjournal.data.BodyMeasurementRepository(
            database.bodyMeasurementDao(),
            profileRepository
        )
        val viewModelFactory = JournalViewModelFactory(application, journalRepository, profileRepository)
        val profileViewModelFactory = ProfileViewModelFactory(profileRepository)
        val measurementViewModelFactory = com.example.healthjournal.viewmodel.BodyMeasurementViewModelFactory(
            measurementRepository,
            journalRepository,
            profileRepository
        )
        val goalsRepository = com.example.healthjournal.data.GoalsRepository(database.goalDao(), profileRepository)
        val personalCardRepository = com.example.healthjournal.data.PersonalCardRepository(database.personalCardDao(), profileRepository)
        val analyticsViewModelFactory = com.example.healthjournal.viewmodel.BodyAnalyticsViewModelFactory(
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
        val exportViewModel = ExportViewModel(application, journalRepository, fullBackupUseCase, profileRepository)
        val restoreViewModel = com.example.healthjournal.export.RestoreViewModel(application)
        val personalCardViewModelFactory = com.example.healthjournal.viewmodel.PersonalCardViewModelFactory(
            personalCardRepository,
            persistUnitSystem = { unitSystem ->
                com.example.healthjournal.data.local.UnitSettings.write(this, unitSystem)
            },
            initialUnitSystem = com.example.healthjournal.data.local.UnitSettings.read(this)
        )
        val workoutRepository = com.example.healthjournal.data.WorkoutRepository(database.workoutSessionDao(), profileRepository)
        val workoutHealthSource = com.example.healthjournal.health.HealthConnectWorkoutDataSource(this)

        // Trigger sync on start
        SyncManager.enqueuePeriodicSync(this)

        setContent {
            HealthJournalTheme {
                val navController = rememberNavController()
                val viewModel: JournalViewModel = viewModel(factory = viewModelFactory)
                val profileViewModel: ProfileViewModel =
                    viewModel(factory = profileViewModelFactory)
                val profileState by profileViewModel.uiState.collectAsState()
                val scope = rememberCoroutineScope()
                val switchProfile: (String) -> Unit = { profileId ->
                    if (profileId != profileState.activeProfile?.id) {
                        scope.launch {
                            val result = profileViewModel.switchTo(profileId)
                            if (result.isSuccess) {
                                navController.popBackStack("main", inclusive = false)
                            }
                        }
                    }
                }
                val manageProfiles = { navController.navigate("manage_profiles") }
                val profileSwitcher: @Composable (Boolean, (String) -> Unit) -> Unit =
                    { enabled, onSwitch ->
                        ProfileSwitcher(
                            profiles = profileState.profiles,
                            activeProfileId = profileState.activeProfile?.id,
                            enabled = enabled,
                            onProfileSelected = onSwitch,
                            onManageProfiles = manageProfiles
                        )
                    }

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            onTileClick = { route -> navController.navigate(route) },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            viewModel = viewModel,
                            onAddEntryClick = { navController.navigate("add_entry") },
                            onEntryClick = { entryId -> navController.navigate("add_entry?entryId=$entryId") },
                            onBack = { navController.popBackStack() },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                    composable("settings") {
                        com.example.healthjournal.ui.screens.SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
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
                                healthSyncEnabled = com.example.healthjournal.data.local.HealthSyncSettings.read(this@MainActivity),
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
                                    database.workoutPresetDao(),
                                    profileRepository
                                ),
                                catalogDao = database.exerciseCatalogDao(),
                                warmupHistoryFormatter = AndroidWarmupHistoryFormatter(this@MainActivity)
                            )
                        }
                        val workoutViewModel: com.example.healthjournal.viewmodel.WorkoutViewModel =
                            viewModel(factory = workoutViewModelFactory)
                        com.example.healthjournal.ui.screens.WorkoutScreen(
                            viewModel = workoutViewModel,
                            onBack = { navController.popBackStack() },
                            onPresetsClick = { navController.navigate("presets") },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                    composable("presets") {
                        val presetFactory = androidx.compose.runtime.remember {
                            com.example.healthjournal.viewmodel.PresetViewModelFactory(
                                repository = com.example.healthjournal.data.PresetRepository(
                                    database.workoutPresetDao(),
                                    profileRepository
                                ),
                                catalogDao = database.exerciseCatalogDao()
                            )
                        }
                        val presetViewModel: com.example.healthjournal.viewmodel.PresetViewModel =
                            androidx.lifecycle.viewmodel.compose.viewModel(factory = presetFactory)
                        com.example.healthjournal.ui.screens.PresetLibraryScreen(
                            viewModel = presetViewModel,
                            onBack = { navController.popBackStack() },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                    composable("measurements") {
                        val measurementViewModel: com.example.healthjournal.viewmodel.BodyMeasurementViewModel =
                            viewModel(factory = measurementViewModelFactory)
                        val analyticsViewModel: com.example.healthjournal.viewmodel.BodyAnalyticsViewModel =
                            viewModel(factory = analyticsViewModelFactory)
                        com.example.healthjournal.ui.screens.MeasurementsScreen(
                            viewModel = measurementViewModel,
                            analyticsViewModel = analyticsViewModel,
                            onBack = { navController.popBackStack() },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                    composable("export") {
                        ExportScreen(
                            viewModel = exportViewModel,
                            restoreViewModel = restoreViewModel,
                            onBack = { navController.popBackStack() },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                    composable("archive") {
                        ArchiveScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onEntryClick = { entryId -> navController.navigate("add_entry?entryId=$entryId") },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
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
                            onBack = { navController.popBackStack() },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                    composable("manage_profiles") {
                        ManageProfilesScreen(
                            viewModel = profileViewModel,
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
                            onBack = { navController.popBackStack() },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                }
            }
        }
    }
}
