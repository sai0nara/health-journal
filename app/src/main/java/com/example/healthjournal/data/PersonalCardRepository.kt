package com.example.healthjournal.data

import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.PersonalCardDao
import kotlinx.coroutines.flow.Flow

class PersonalCardRepository(private val dao: PersonalCardDao) {

    companion object {
        const val PERSONAL_CARD_ID = "personal_card"
    }

    fun getPersonalCard(): Flow<PersonalCard?> {
        return dao.getPersonalCard(PERSONAL_CARD_ID)
    }

    suspend fun getPersonalCardSnapshot(): PersonalCard? {
        return dao.getPersonalCardSnapshot(PERSONAL_CARD_ID)
    }

    suspend fun insertOrUpdate(card: PersonalCard) {
        val cardWithId = card.copy(id = PERSONAL_CARD_ID)
        dao.insertOrUpdate(cardWithId)
    }

    suspend fun deletePersonalCard() {
        dao.deletePersonalCard(PERSONAL_CARD_ID)
    }

    suspend fun getPendingSyncEntries(): List<PersonalCard> {
        return dao.getPendingSyncEntries()
    }

    suspend fun updateSyncStatus(syncStatus: String) {
        dao.updateSyncStatus(PERSONAL_CARD_ID, syncStatus)
    }

    suspend fun markEntryDirty() {
        dao.markEntryDirty(PERSONAL_CARD_ID, System.currentTimeMillis())
    }
}
