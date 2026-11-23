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
        val restoreViewModelFactory = com.example.healthjournal.export.RestoreViewModel.Factory(application)
        val personalCardViewModelFactory = com.example.healthjournal.viewmodel.PersonalCardViewModelFactory(
            personalCardRepository,
            persistUnitSystem = { unitSystem ->
                com.example.healthjournal.data.local.UnitSettings.write(this, unitSystem)
            },
            initialUnitSystem = com.example.healthjournal.data.local.UnitSettings.read(this)
        )
        val vitalsUnitSystemFlow = kotlinx.coroutines.flow.MutableStateFlow(
            com.example.healthjournal.data.local.UnitSettings.read(this)
        )
        val vitalsViewModelFactory = com.example.healthjournal.viewmodel.VitalsViewModelFactory(
            journalRepository = journalRepository,
            activeProfileProvider = profileRepository,
            unitSystemFlow = vitalsUnitSystemFlow
        )
        val workoutRepository = com.example.healthjournal.data.WorkoutRepository(database.workoutSessionDao(), profileRepository)
        val presetRepository = com.example.healthjournal.data.PresetRepository(database.workoutPresetDao(), profileRepository)
        val workoutHealthSource = com.example.healthjournal.health.HealthConnectWorkoutDataSource(this)
        val todayDashboardViewModelFactory = com.example.healthjournal.viewmodel.TodayDashboardViewModel.Factory(
            journalRepository = journalRepository,
            workoutRepository = workoutRepository,
            measurementRepository = measurementRepository,
            goalsRepository = goalsRepository,
            presetRepository = presetRepository,
            activeProfileProvider = profileRepository,
            unitSystemFlow = vitalsUnitSystemFlow
        )

        // Trigger sync on start
        SyncManager.enqueuePeriodicSync(this)

        setContent {
            HealthJournalTheme {
                val navController = rememberNavController()
                val viewModel: JournalViewModel = viewModel(factory = viewModelFactory)
                val todayDashboardViewModel: com.example.healthjournal.viewmodel.TodayDashboardViewModel =
                    viewModel(factory = todayDashboardViewModelFactory)
                val todayState by todayDashboardViewModel.uiState.collectAsState()
                val profileViewModel: ProfileViewModel =
                    viewModel(factory = profileViewModelFactory)
                val restoreViewModel: com.example.healthjournal.export.RestoreViewModel =
                    viewModel(factory = restoreViewModelFactory)
                val profileState by profileViewModel.uiState.collectAsState()
                val scope = rememberCoroutineScope()
                val pendingVitalTab = androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf<com.example.healthjournal.viewmodel.VitalMetricTab?>(null)
                }
                val pendingPresetId = androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf<String?>(null)
                }
                val navigateTopLevelTab: (String) -> Unit = { route ->
                    navController.navigate(route) {
                        popUpTo("main") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                val bottomBarForRoute: (String) -> @Composable () -> Unit = { route ->
                    {
                        com.example.healthjournal.ui.components.TodayBottomBar(
                            currentRoute = route,
                            onNavigate = navigateTopLevelTab
                        )
                    }
                }
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
                val openPersonalCard = { navController.navigate("personal_card") }
                val profileSwitcher: @Composable (Boolean, (String) -> Unit) -> Unit =
                    { enabled, onSwitch ->
                        ProfileSwitcher(
                            profiles = profileState.profiles,
                            activeProfileId = profileState.activeProfile?.id,
                            enabled = enabled,
                            onProfileSelected = onSwitch,
                            onManageProfiles = manageProfiles,
                            onPersonalCard = openPersonalCard
                        )
                    }

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            state = todayState,
                            onOpenVitals = { tab ->
                                pendingVitalTab.value = tab
                                navigateTopLevelTab("vitals")
                            },
                            onOpenWorkouts = { navController.navigate("workout") },
                            onOpenMeasurements = { navController.navigate("measurements") },
                            onOpenHistory = { navigateTopLevelTab("history") },
                            onOpenPresets = { navController.navigate("presets") },
                            onStartPresetWorkout = { preset ->
                                pendingPresetId.value = preset.id
                                navController.navigate("workout")
                            },
                            onNewJournalEntry = { navController.navigate("add_entry") },
                            onActivityClick = { item ->
                                when (item.category) {
                                    com.example.healthjournal.viewmodel.TodayActivityCategory.VITALS -> {
                                        pendingVitalTab.value = item.vitalTab ?: com.example.healthjournal.viewmodel.VitalMetricTab.BLOOD_PRESSURE
                                        navigateTopLevelTab("vitals")
                                    }
                                    com.example.healthjournal.viewmodel.TodayActivityCategory.WORKOUT ->
                                        navController.navigate("workout")
                                    com.example.healthjournal.viewmodel.TodayActivityCategory.MEASUREMENT ->
                                        navController.navigate("measurements")
                                    com.example.healthjournal.viewmodel.TodayActivityCategory.JOURNAL -> {
                                        val entryId = item.id.removePrefix("j_")
                                        navController.navigate("add_entry?entryId=$entryId")
                                    }
                                }
                            },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile,
                            bottomBar = bottomBarForRoute("main")
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            viewModel = viewModel,
                            onAddEntryClick = { navController.navigate("add_entry") },
                            onEntryClick = { entryId -> navController.navigate("add_entry?entryId=$entryId") },
                            onBack = { navController.popBackStack() },
                            onOpenArchive = { navController.navigate("archive") },
                            bottomBar = bottomBarForRoute("history"),
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile
                        )
                    }
                    composable("more") {
                        com.example.healthjournal.ui.screens.MoreScreen(
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenExport = { navController.navigate("export") },
                            onOpenPresets = { navController.navigate("presets") },
                            profileSwitcher = profileSwitcher,
                            onSwitchProfile = switchProfile,
                            bottomBar = bottomBarForRoute("more")
                        )
                    }
                    composable("settings") {
                        val weeklyTarget by goalsRepository.workoutWeeklyTarget.collectAsState(
                            initial = com.example.healthjournal.data.GoalsRepository.DEFAULT_WORKOUT_WEEKLY_TARGET
                        )
                        com.example.healthjournal.ui.screens.SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            weeklyWorkoutTarget = weeklyTarget,
                            onWeeklyWorkoutTargetChange = { newTarget ->
                                scope.launch { goalsRepository.setWorkoutWeeklyTarget(newTarget) }
                            },
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
                        val weeklyTarget by goalsRepository.workoutWeeklyTarget.collectAsState(
                            initial = com.example.healthjournal.data.GoalsRepository.DEFAULT_WORKOUT_WEEKLY_TARGET
                        )
                        val presetToStart = pendingPresetId.value
                        androidx.compose.runtime.LaunchedEffect(presetToStart) {
                            if (presetToStart != null) {
                                workoutViewModel.startPreset(presetToStart)
                                pendingPresetId.value = null
                            }
                        }
                        com.example.healthjournal.ui.screens.WorkoutScreen(
                            viewModel = workoutViewModel,
                            onBack = { navController.popBackStack() },
                            onPresetsClick = { navController.navigate("presets") },
                            weeklyWorkoutTarget = weeklyTarget,
                            onWeeklyWorkoutTargetChange = { newTarget ->
                                scope.launch { goalsRepository.setWorkoutWeeklyTarget(newTarget) }
                            },
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
                    composable("vitals") {
                        val vitalsViewModel: com.example.healthjournal.viewmodel.VitalsViewModel =
                            viewModel(factory = vitalsViewModelFactory)
                        val initialTab = pendingVitalTab.value
                        androidx.compose.runtime.LaunchedEffect(initialTab) {
                            if (initialTab != null) {
                                vitalsViewModel.selectMetricTab(initialTab)
                                pendingVitalTab.value = null
                            }
                        }
                        com.example.healthjournal.ui.screens.VitalsScreen(
                            viewModel = vitalsViewModel,
                            onBack = { navController.popBackStack() },
                            initialMetric = initialTab,
                            bottomBar = bottomBarForRoute("vitals"),
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
