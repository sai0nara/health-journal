package com.example.healthjournal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.healthjournal.data.local.BloodType
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PersonalCardDaoTest {

    private lateinit var personalCardDao: PersonalCardDao
    private lateinit var db: JournalDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, JournalDatabase::class.java
        ).build()
        personalCardDao = db.personalCardDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetPersonalCard() = runBlocking {
        val card = PersonalCard(
            id = "personal_card",
            demographics = Demographics(fullName = "John Doe", heightCm = 180.0)
        )
        personalCardDao.insertOrUpdate(card)

        val loaded = personalCardDao.getPersonalCardSnapshot("personal_card")

        assertEquals("John Doe", loaded?.demographics?.fullName)
        assertEquals(180.0, loaded?.demographics?.heightCm)
    }

    @Test
    fun insertOrUpdate_upsertsExistingCard() = runBlocking {
        val card = PersonalCard(
            id = "personal_card",
            demographics = Demographics(fullName = "John Doe")
        )
        personalCardDao.insertOrUpdate(card)

        val updated = card.copy(
            demographics = Demographics(fullName = "Jane Doe"),
            lastModified = System.currentTimeMillis()
        )
        personalCardDao.insertOrUpdate(updated)

        val loaded = personalCardDao.getPersonalCardSnapshot("personal_card")
        assertEquals("Jane Doe", loaded?.demographics?.fullName)
    }

    @Test
    fun getPersonalCard_returnsFlow() = runBlocking {
        val card = PersonalCard(
            id = "personal_card",
            demographics = Demographics(fullName = "John Doe")
        )
        personalCardDao.insertOrUpdate(card)

        val flowValue = personalCardDao.getPersonalCard("personal_card").first()

        assertEquals("John Doe", flowValue?.demographics?.fullName)
    }

    @Test
    fun getPersonalCardSnapshot_returnsNullForNonExistent() = runBlocking {
        assertNull(personalCardDao.getPersonalCardSnapshot("non_existent"))
    }

    @Test
    fun deletePersonalCard_removesRow() = runBlocking {
        val card = PersonalCard(id = "personal_card")
        personalCardDao.insertOrUpdate(card)

        personalCardDao.deletePersonalCard("personal_card")

        assertNull(personalCardDao.getPersonalCardSnapshot("personal_card"))
    }

    @Test
    fun getPendingSyncEntries_onlyReturnsPendingRows() = runBlocking {
        personalCardDao.insertOrUpdate(
            PersonalCard(id = "card1", syncStatus = "PENDING_SYNC")
        )
        personalCardDao.insertOrUpdate(
            PersonalCard(id = "card2", syncStatus = "SYNCED")
        )

        val pending = personalCardDao.getPendingSyncEntries()

        assertEquals(1, pending.size)
        assertEquals("card1", pending[0].id)
    }

    @Test
    fun updateSyncStatus_updatesCard() = runBlocking {
        personalCardDao.insertOrUpdate(
            PersonalCard(id = "personal_card", syncStatus = "PENDING_SYNC")
        )

        personalCardDao.updateSyncStatus("personal_card", "SYNCED")

        val loaded = personalCardDao.getPersonalCardSnapshot("personal_card")
        assertEquals("SYNCED", loaded?.syncStatus)
    }

    @Test
    fun markEntryDirty_resetsSyncStatusAndBumpsLastModified() = runBlocking {
        personalCardDao.insertOrUpdate(
            PersonalCard(id = "personal_card", lastModified = 1_000L, syncStatus = "SYNCED")
        )

        personalCardDao.markEntryDirty("personal_card", 9_000L)

        val dirty = personalCardDao.getPersonalCardSnapshot("personal_card")!!
        assertEquals("PENDING_SYNC", dirty.syncStatus)
        assertEquals(9_000L, dirty.lastModified)
    }

    @Test
    fun medicalProfile_serializesCorrectly() = runBlocking {
        val card = PersonalCard(
            id = "personal_card",
            medicalProfile = MedicalProfile(
                bloodType = BloodType.O_POSITIVE,
                allergies = listOf("Penicillin", "Peanuts"),
                medications = listOf(
                    MedicationEntry(name = "Aspirin", dosage = "81mg", schedule = "Daily")
                )
            )
        )
        personalCardDao.insertOrUpdate(card)

        val loaded = personalCardDao.getPersonalCardSnapshot("personal_card")

        assertEquals(BloodType.O_POSITIVE, loaded?.medicalProfile?.bloodType)
        assertEquals(2, loaded?.medicalProfile?.allergies?.size)
        assertEquals("Aspirin", loaded?.medicalProfile?.medications?.first()?.name)
    }

    @Test
    fun emergencyContacts_serializesCorrectly() = runBlocking {
        val card = PersonalCard(
            id = "personal_card",
            emergencyContacts = EmergencyContacts(
                contacts = listOf(
                    EmergencyContact(name = "Jane Doe", relationship = "Spouse", phoneNumber = "555-0123")
                )
            )
        )
        personalCardDao.insertOrUpdate(card)

        val loaded = personalCardDao.getPersonalCardSnapshot("personal_card")

        assertEquals(1, loaded?.emergencyContacts?.contacts?.size)
        assertEquals("Jane Doe", loaded?.emergencyContacts?.contacts?.first()?.name)
        assertEquals("Spouse", loaded?.emergencyContacts?.contacts?.first()?.relationship)
    }
}