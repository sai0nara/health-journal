package com.example.healthjournal.data

import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.PersonalCardDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalCardRepository(
    private val dao: PersonalCardDao,
    private val activeProfile: ActiveProfileProvider
) {

    companion object {
        const val PERSONAL_CARD_ID = "personal_card"
    }

    fun currentProfileId(): String = activeProfile.currentProfileId()

    fun getPersonalCard(): Flow<PersonalCard?> =
        activeProfile.activeProfileId.flatMapLatest {
            dao.getPersonalCard(it, PERSONAL_CARD_ID)
        }

    suspend fun getPersonalCardSnapshot(): PersonalCard? =
        getPersonalCardSnapshot(activeProfile.currentProfileId())

    suspend fun getPersonalCardSnapshot(profileId: String): PersonalCard? =
        dao.getPersonalCardSnapshot(profileId, PERSONAL_CARD_ID)

    suspend fun insertOrUpdate(card: PersonalCard) {
        insertOrUpdate(activeProfile.currentProfileId(), card)
    }

    /** Writes [card] under [profileId] so a profile switch cannot redirect an open editor. */
    suspend fun insertOrUpdate(profileId: String, card: PersonalCard) {
        val cardWithOwner = card.copy(
            id = PERSONAL_CARD_ID,
            profileId = profileId
        )
        dao.insertOrUpdate(cardWithOwner)
    }

    suspend fun deletePersonalCard() {
        dao.deletePersonalCard(activeProfile.currentProfileId(), PERSONAL_CARD_ID)
    }

    suspend fun getPendingSyncEntries(): List<PersonalCard> =
        getPendingSyncEntries(activeProfile.currentProfileId())

    suspend fun getPendingSyncEntries(profileId: String): List<PersonalCard> =
        dao.getPendingSyncEntries(profileId)

    suspend fun updateSyncStatus(syncStatus: String) {
        dao.updateSyncStatus(activeProfile.currentProfileId(), PERSONAL_CARD_ID, syncStatus)
    }

    suspend fun markEntryDirty() {
        markEntryDirty(activeProfile.currentProfileId())
    }

    suspend fun markEntryDirty(profileId: String) {
        dao.markEntryDirty(
            profileId,
            PERSONAL_CARD_ID,
            System.currentTimeMillis()
        )
    }
}
