package com.example.healthjournal.data

interface ProfileDataEraser {
    suspend fun erase(profileId: String)
}
