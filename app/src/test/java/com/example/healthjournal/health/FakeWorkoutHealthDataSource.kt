package com.example.healthjournal.health

/**
 * In-memory [WorkoutHealthDataSource] for JVM unit tests (e.g. ViewModel
 * tests): records written while [permissionGranted] is true are readable
 * back; writes without permission fail without storing anything.
 */
class FakeWorkoutHealthDataSource(
    var permissionGranted: Boolean = true
) : WorkoutHealthDataSource {

    private val stored = mutableListOf<WorkoutHealthRecord>()

    override suspend fun hasPermissions(): Boolean = permissionGranted

    override suspend fun writeRecord(record: WorkoutHealthRecord): Boolean {
        if (!permissionGranted) return false
        stored.add(record)
        return true
    }

    override suspend fun readRecent(daysBack: Int): List<WorkoutHealthRecord> =
        stored.toList()

    fun storedRecords(): List<WorkoutHealthRecord> = stored.toList()
}
