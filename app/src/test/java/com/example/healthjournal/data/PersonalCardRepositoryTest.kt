package com.example.healthjournal.data

import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.PersonalCardDao
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class PersonalCardRepositoryTest {

    private lateinit var repository: PersonalCardRepository
    private lateinit var activeProfile: FakeActiveProfileProvider
    private val dao: PersonalCardDao = mockk()

    @Before
    fun setup() {
        activeProfile = FakeActiveProfileProvider()
        repository = PersonalCardRepository(dao, activeProfile)
    }

    @Test
    fun insertOrUpdate_withExplicitProfileStampsOwner() = runBlocking {
        val card = PersonalCard()
        coEvery { dao.insertOrUpdate(any()) } returns Unit

        repository.insertOrUpdate("sibling", card)

        coVerify {
            dao.insertOrUpdate(
                card.copy(
                    id = PersonalCardRepository.PERSONAL_CARD_ID,
                    profileId = "sibling"
                )
            )
        }
    }

    @Test
    fun insertOrUpdate_withoutProfileUsesActiveProfile() = runBlocking {
        val card = PersonalCard()
        coEvery { dao.insertOrUpdate(any()) } returns Unit

        repository.insertOrUpdate(card)

        coVerify {
            dao.insertOrUpdate(
                card.copy(
                    id = PersonalCardRepository.PERSONAL_CARD_ID,
                    profileId = SELF_PROFILE_ID
                )
            )
        }
    }

    @Test
    fun markEntryDirty_withExplicitProfileUsesThatProfile() = runBlocking {
        coEvery { dao.markEntryDirty(any(), any(), any()) } returns Unit

        repository.markEntryDirty("sibling")

        coVerify {
            dao.markEntryDirty(
                "sibling",
                PersonalCardRepository.PERSONAL_CARD_ID,
                match { it > 0L }
            )
        }
    }
}
