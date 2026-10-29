/*
 * Copyright (c) 2026 Kaan Gül
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.focusmode.impl.session

import com.duckduckgo.focusmode.impl.history.FocusHistoryRepository
import com.duckduckgo.focusmode.impl.history.FocusSession
import com.duckduckgo.focusmode.impl.store.FocusStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class FocusSessionControllerTest {

    private val store = FakeFocusStore()
    private val history = FakeHistory()
    private val clock = FakeClock(now = 1_000_000L)
    private val controller = FocusSessionController(store, history, clock)

    @Test
    fun whenAppIconLaunchThenAwaitingSelection() = runTest {
        controller.onBrowserResumed("standard")

        assertEquals(FocusPhase.AwaitingSelection, controller.state.value?.phase)
    }

    @Test
    fun whenLinkLaunchThenGraceUntilThreeMinutesPass() = runTest {
        controller.onBrowserResumed("url")
        assertEquals(FocusPhase.Grace, controller.state.value?.phase)

        clock.now += 3.minutes.inWholeMilliseconds
        controller.tick()

        assertEquals(FocusPhase.AwaitingSelection, controller.state.value?.phase)
    }

    @Test
    fun whenDurationSelectedThenSessionIsPersisted() = runTest {
        controller.onBrowserResumed("standard")
        controller.selectDuration(15.minutes)

        val saved = store.session
        assertEquals(FocusPhase.Running, saved.state.phase)
        assertEquals(clock.now + 15.minutes.inWholeMilliseconds, saved.state.endsAtMs)
        assertEquals(15.minutes.inWholeMilliseconds, saved.meta.plannedMs)
    }

    @Test
    fun whenControllerRestartsThenPersistedSessionIsResumed() = runTest {
        controller.onBrowserResumed("standard")
        controller.selectDuration(15.minutes)

        val restarted = FocusSessionController(store, history, clock)
        restarted.onBrowserResumed("url")

        assertEquals(FocusPhase.Running, restarted.state.value?.phase)
    }

    @Test
    fun whenSessionEndsWithoutExtensionThenItIsRecordedAtItsPlannedEnd() = runTest {
        store.settings = FocusSettings(extensionEnabled = false)
        controller.onBrowserResumed("standard")
        controller.selectDuration(10.minutes)
        val startedAt = clock.now

        clock.now += 30.minutes.inWholeMilliseconds
        controller.tick()

        assertEquals(FocusPhase.Expired, controller.state.value?.phase)
        assertEquals(
            listOf(FocusSession(startedAt, startedAt + 10.minutes.inWholeMilliseconds, 10.minutes.inWholeMilliseconds, extended = false)),
            history.recorded,
        )
    }

    @Test
    fun whenExtendedThenHistoryHoldsOneSessionWithBothLengths() = runTest {
        controller.onBrowserResumed("standard")
        controller.selectDuration(10.minutes)
        clock.now += 10.minutes.inWholeMilliseconds
        controller.tick()
        assertEquals(FocusPhase.ExtensionPrompt, controller.state.value?.phase)
        assertTrue(history.recorded.isEmpty())

        controller.extend()
        clock.now += 5.minutes.inWholeMilliseconds
        controller.tick()

        assertEquals(FocusPhase.Expired, controller.state.value?.phase)
        assertEquals(1, history.recorded.size)
        assertEquals(15.minutes.inWholeMilliseconds, history.recorded.single().plannedMs)
        assertTrue(history.recorded.single().extended)
    }

    @Test
    fun whenClosedWhileAwaitingSelectionThenNothingIsRecorded() = runTest {
        controller.onBrowserResumed("standard")
        controller.close()

        assertEquals(FocusPhase.Expired, controller.state.value?.phase)
        assertTrue(history.recorded.isEmpty())
    }

    @Test
    fun whenClosedAtExtensionPromptThenSessionIsRecorded() = runTest {
        controller.onBrowserResumed("standard")
        controller.selectDuration(5.minutes)
        clock.now += 5.minutes.inWholeMilliseconds
        controller.tick()

        controller.close()

        assertEquals(1, history.recorded.size)
    }

    private class FakeClock(var now: Long) : FocusClock {
        override fun nowMs(): Long = now
    }

    private class FakeHistory : FocusHistoryRepository {
        val recorded = mutableListOf<FocusSession>()
        override fun sessions(): Flow<List<FocusSession>> = flowOf(recorded.toList())
        override suspend fun record(session: FocusSession) {
            recorded += session
        }
        override suspend fun clear() = recorded.clear()
    }

    private class FakeFocusStore : FocusStore {
        var session = PersistedSession()
        var settings = FocusSettings()
        private val settingsFlow = MutableStateFlow(settings)

        override suspend fun loadSession(): PersistedSession = session
        override suspend fun saveSession(session: PersistedSession) {
            this.session = session
        }
        override fun settings(): Flow<FocusSettings> = settingsFlow
        override suspend fun currentSettings(): FocusSettings = settings
        override suspend fun setExtensionEnabled(enabled: Boolean) = Unit
        override suspend fun setExtensionDuration(duration: Duration) = Unit
        override suspend fun setGraceDuration(duration: Duration) = Unit
    }
}
