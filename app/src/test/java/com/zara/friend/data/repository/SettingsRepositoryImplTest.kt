package com.zara.friend.data.repository

import app.cash.turbine.test
import com.zara.friend.data.local.dao.SettingsDao
import com.zara.friend.data.local.entity.SettingsEntity
import com.zara.friend.data.mapper.toDomain
import com.zara.friend.data.mapper.toEntity
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.ThemeMode
import com.zara.friend.domain.model.UserSettings
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryImplTest {

    private lateinit var dao: SettingsDao
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxUnitFun = true)
        repository = SettingsRepositoryImpl(dao)
    }

    @Test
    fun `observeSettings emits defaults when dao row is null`() = runTest {
        every { dao.observe() } returns flowOf(null)

        repository.observeSettings().test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals(PersonaMode.AKU_KAMU, settings.personaMode)
            assertEquals(true, settings.voiceAutoPlay)
            assertEquals(true, settings.syncEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSettings emits mapped row when present`() = runTest {
        every { dao.observe() } returns flowOf(
            SettingsEntity(
                id = 1,
                themeMode = ThemeMode.DARK.name,
                personaMode = PersonaMode.GUE_LO.name,
                voiceAutoPlay = false,
                syncEnabled = true,
                anonymousUid = "uid-1",
            ),
        )

        repository.observeSettings().test {
            val settings = awaitItem()
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertEquals(PersonaMode.GUE_LO, settings.personaMode)
            assertEquals(false, settings.voiceAutoPlay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getSettings returns defaults when row absent`() = runTest {
        io.mockk.coEvery { dao.get() } returns null

        val settings = repository.getSettings()

        assertEquals(PersonaMode.AKU_KAMU, settings.personaMode)
    }

    @Test
    fun `getSettings returns mapped row`() = runTest {
        io.mockk.coEvery { dao.get() } returns SettingsEntity(
            id = 1,
            themeMode = ThemeMode.LIGHT.name,
            personaMode = PersonaMode.AKU_KAMU.name,
            voiceAutoPlay = true,
            syncEnabled = false,
            anonymousUid = null,
        )

        assertEquals(ThemeMode.LIGHT, repository.getSettings().themeMode)
    }

    @Test
    fun `updateSettings upserts single row entity`() = runTest {
        val settings = UserSettings(
            themeMode = ThemeMode.DARK,
            personaMode = PersonaMode.GUE_LO,
            voiceAutoPlay = false,
            syncEnabled = true,
        )
        val slot = slot<SettingsEntity>()

        val result = repository.updateSettings(settings)

        assertTrue(result.isSuccess)
        coVerify { dao.upsert(capture(slot)) }
        assertEquals(1, slot.captured.id)
        assertEquals(ThemeMode.DARK.name, slot.captured.themeMode)
        assertEquals(PersonaMode.GUE_LO.name, slot.captured.personaMode)
        assertEquals(false, slot.captured.voiceAutoPlay)
        assertEquals(true, slot.captured.syncEnabled)
    }

    @Test
    fun `updateAnonymousUid delegates to dao`() = runTest {
        val result = repository.updateAnonymousUid("uid-42")

        assertTrue(result.isSuccess)
        coVerify { dao.updateAnonymousUid("uid-42") }
    }

    @Test
    fun `settings round trip is lossless`() = runTest {
        val settings = UserSettings(
            themeMode = ThemeMode.LIGHT,
            personaMode = PersonaMode.GUE_LO,
            voiceAutoPlay = true,
            syncEnabled = false,
        )

        assertEquals(settings, settings.toEntity().toDomain())
    }
}
