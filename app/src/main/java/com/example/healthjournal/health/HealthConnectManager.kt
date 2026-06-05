package com.example.healthjournal.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    fun checkAvailability(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    suspend fun hasAllPermissions(): Boolean {
        android.util.Log.d("HealthConnectManager", "Checking permissions for: $requiredPermissions")
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        val hasAll = granted.containsAll(requiredPermissions)
        android.util.Log.d("HealthConnectManager", "Has all permissions: $hasAll (Granted: $granted)")
        return hasAll
    }

    suspend fun getLatestBloodPressure(startTime: Instant, endTime: Instant): Pair<Double, Double>? {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    ascendingOrder = false
                )
            )
            val latest = response.records.firstOrNull()
            if (latest != null) {
                latest.systolic.inMillimetersOfMercury to latest.diastolic.inMillimetersOfMercury
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAverageHeartRate(startTime: Instant, endTime: Instant): Long? {
        return try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response[HeartRateRecord.BPM_AVG]
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getSleepDurationHours(startTime: Instant, endTime: Instant): Float? {
        return try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val duration = response[SleepSessionRecord.SLEEP_DURATION_TOTAL]
            duration?.toMinutes()?.let { it / 60f }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
