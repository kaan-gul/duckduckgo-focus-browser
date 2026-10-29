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

import kotlin.time.Duration

class FocusSessionMachine(private val settings: FocusSettings = FocusSettings()) {

    fun onLaunch(
        state: FocusSessionState,
        source: LaunchSource,
        nowMs: Long,
    ): FocusSessionState {
        val current = advance(state, nowMs)
        if (current.phase.isActive) return current

        return when (source) {
            LaunchSource.Manual -> FocusSessionState(FocusPhase.AwaitingSelection)
            LaunchSource.External -> FocusSessionState(FocusPhase.Grace, nowMs + settings.graceDuration.inWholeMilliseconds)
        }
    }

    fun advance(
        state: FocusSessionState,
        nowMs: Long,
    ): FocusSessionState {
        if (nowMs < state.endsAtMs) return state

        return when (state.phase) {
            FocusPhase.Grace -> FocusSessionState(FocusPhase.AwaitingSelection)
            FocusPhase.Running ->
                if (canExtend(state)) {
                    state.copy(phase = FocusPhase.ExtensionPrompt, endsAtMs = 0L)
                } else {
                    FocusSessionState(FocusPhase.Expired)
                }
            else -> state
        }
    }

    fun onDurationSelected(
        state: FocusSessionState,
        duration: Duration,
        nowMs: Long,
    ): FocusSessionState {
        if (state.phase != FocusPhase.AwaitingSelection || !duration.isPositive()) return state
        return FocusSessionState(FocusPhase.Running, nowMs + duration.inWholeMilliseconds)
    }

    fun onExtendRequested(
        state: FocusSessionState,
        nowMs: Long,
    ): FocusSessionState {
        if (state.phase != FocusPhase.ExtensionPrompt || !canExtend(state)) return state
        return FocusSessionState(FocusPhase.Running, nowMs + settings.extensionDuration.inWholeMilliseconds, extensionUsed = true)
    }

    fun onCloseRequested(): FocusSessionState = FocusSessionState(FocusPhase.Expired)

    fun warningAtMs(state: FocusSessionState): Long? =
        if (state.phase == FocusPhase.Running) state.endsAtMs - settings.warningLead.inWholeMilliseconds else null

    private fun canExtend(state: FocusSessionState): Boolean = settings.extensionEnabled && !state.extensionUsed
}
