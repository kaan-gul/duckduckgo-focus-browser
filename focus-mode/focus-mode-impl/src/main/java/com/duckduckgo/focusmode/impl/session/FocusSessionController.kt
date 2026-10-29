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

import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.focusmode.impl.history.FocusHistoryRepository
import com.duckduckgo.focusmode.impl.history.FocusSession
import com.duckduckgo.focusmode.impl.store.FocusStore
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Owns the single focus session: applies [FocusSessionMachine] transitions, persists the result and records finished
 * sessions in the history. All mutations are serialised so a timer tick and a user action can never interleave.
 */
@SingleIn(AppScope::class)
class FocusSessionController @Inject constructor(
    private val store: FocusStore,
    private val history: FocusHistoryRepository,
    private val clock: FocusClock,
) {

    private val mutex = Mutex()
    private var session: PersistedSession? = null

    private val _state = MutableStateFlow<FocusSessionState?>(null)

    /** `null` until the persisted session has been read for the first time. */
    val state: StateFlow<FocusSessionState?> = _state.asStateFlow()

    suspend fun currentSettings(): FocusSettings = store.currentSettings()

    suspend fun onBrowserResumed(launchSourceValue: String?) = mutate { current, machine, now ->
        machine.onLaunch(current, FocusLaunchClassifier.classify(launchSourceValue), now)
    }

    suspend fun tick() = mutate { current, machine, now -> machine.advance(current, now) }

    suspend fun selectDuration(duration: Duration) = mutate { current, machine, now ->
        machine.onDurationSelected(current, duration, now)
    }

    suspend fun extend() = mutate { current, machine, now -> machine.onExtendRequested(current, now) }

    suspend fun close() = mutate { _, machine, _ -> machine.onCloseRequested() }

    private suspend fun mutate(transition: (FocusSessionState, FocusSessionMachine, Long) -> FocusSessionState) {
        mutex.withLock {
            val before = session ?: store.loadSession().also { session = it }
            val now = clock.nowMs()
            val machine = FocusSessionMachine(store.currentSettings())
            val next = transition(before.state, machine, now)

            if (next != before.state) {
                val after = PersistedSession(next, metaAfter(before, next, now))
                recordIfFinished(before, next)
                store.saveSession(after)
                session = after
            }
            _state.value = next
        }
    }

    private fun metaAfter(
        before: PersistedSession,
        next: FocusSessionState,
        now: Long,
    ): FocusSessionMeta = when {
        before.state.phase == FocusPhase.AwaitingSelection && next.phase == FocusPhase.Running ->
            FocusSessionMeta(startedAtMs = now, plannedMs = next.endsAtMs - now)
        before.state.phase == FocusPhase.ExtensionPrompt && next.phase == FocusPhase.Running ->
            before.meta.copy(plannedMs = before.meta.plannedMs + (next.endsAtMs - now), extended = true)
        next.phase.isInSession() -> before.meta
        else -> FocusSessionMeta()
    }

    private suspend fun recordIfFinished(
        before: PersistedSession,
        next: FocusSessionState,
    ) {
        if (!before.state.phase.isInSession() || next.phase.isInSession()) return
        // The planned end, not "now": a session that lapsed while the app was closed must not be credited with the time away.
        history.record(
            FocusSession(
                startedAtMs = before.meta.startedAtMs,
                endedAtMs = before.meta.plannedEndMs,
                plannedMs = before.meta.plannedMs,
                extended = before.meta.extended,
            ),
        )
    }

    private fun FocusPhase.isInSession() = this == FocusPhase.Running || this == FocusPhase.ExtensionPrompt
}
