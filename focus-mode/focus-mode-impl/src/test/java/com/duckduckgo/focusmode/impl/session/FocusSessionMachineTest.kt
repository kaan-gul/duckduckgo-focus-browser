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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FocusSessionMachineTest {

    private val machine = FocusSessionMachine()
    private val now = 1_000_000L
    private val idle = FocusSessionState()

    private fun running(endsAtMs: Long, extensionUsed: Boolean = false) = FocusSessionState(FocusPhase.Running, endsAtMs, extensionUsed)

    @Test
    fun whenManualLaunchWithoutSessionThenAwaitingSelection() {
        assertEquals(FocusPhase.AwaitingSelection, machine.onLaunch(idle, LaunchSource.Manual, now).phase)
    }

    @Test
    fun whenExternalLaunchWithoutSessionThenThreeMinuteGrace() {
        val state = machine.onLaunch(idle, LaunchSource.External, now)

        assertEquals(FocusSessionState(FocusPhase.Grace, now + 3.minutes.inWholeMilliseconds), state)
    }

    @Test
    fun whenLaunchedAfterExpiryThenNewSessionStarts() {
        val state = machine.onLaunch(FocusSessionState(FocusPhase.Expired), LaunchSource.Manual, now)

        assertEquals(FocusPhase.AwaitingSelection, state.phase)
    }

    @Test
    fun whenLaunchedWhileRunningThenSessionIsKept() {
        val current = running(now + 1.minutes.inWholeMilliseconds)

        assertEquals(current, machine.onLaunch(current, LaunchSource.Manual, now))
        assertEquals(current, machine.onLaunch(current, LaunchSource.External, now))
    }

    @Test
    fun whenExternalLaunchWhileAwaitingSelectionThenNoNewGrace() {
        val awaiting = FocusSessionState(FocusPhase.AwaitingSelection)

        assertEquals(awaiting, machine.onLaunch(awaiting, LaunchSource.External, now))
    }

    @Test
    fun whenGraceEndedWhileAppWasClosedThenRelaunchAwaitsSelection() {
        val grace = FocusSessionState(FocusPhase.Grace, now - 1)

        assertEquals(FocusPhase.AwaitingSelection, machine.onLaunch(grace, LaunchSource.External, now).phase)
    }

    @Test
    fun whenGraceNotYetEndedThenAdvanceKeepsIt() {
        val grace = FocusSessionState(FocusPhase.Grace, now + 1)

        assertEquals(grace, machine.advance(grace, now))
    }

    @Test
    fun whenGraceEndsThenAwaitingSelection() {
        val grace = FocusSessionState(FocusPhase.Grace, now)

        assertEquals(FocusSessionState(FocusPhase.AwaitingSelection), machine.advance(grace, now))
    }

    @Test
    fun whenDurationSelectedThenRunningWithoutExtensionUsed() {
        val state = machine.onDurationSelected(FocusSessionState(FocusPhase.AwaitingSelection), 15.minutes, now)

        assertEquals(running(now + 15.minutes.inWholeMilliseconds), state)
    }

    @Test
    fun whenDurationSelectedOutsideSelectionThenIgnored() {
        val current = running(now + 1.minutes.inWholeMilliseconds)

        assertEquals(current, machine.onDurationSelected(current, 2.minutes, now))
    }

    @Test
    fun whenDurationNotPositiveThenIgnored() {
        val awaiting = FocusSessionState(FocusPhase.AwaitingSelection)

        assertEquals(awaiting, machine.onDurationSelected(awaiting, 0.seconds, now))
        assertEquals(awaiting, machine.onDurationSelected(awaiting, (-1).minutes, now))
    }

    @Test
    fun whenRunningEndsWithExtensionAvailableThenExtensionPrompt() {
        val state = machine.advance(running(now), now)

        assertEquals(FocusPhase.ExtensionPrompt, state.phase)
    }

    @Test
    fun whenRunningEndsAfterExtensionUsedThenExpired() {
        assertEquals(FocusPhase.Expired, machine.advance(running(now, extensionUsed = true), now).phase)
    }

    @Test
    fun whenExtensionDisabledThenRunningEndsExpired() {
        val noExtension = FocusSessionMachine(FocusSettings(extensionEnabled = false))

        assertEquals(FocusPhase.Expired, noExtension.advance(running(now), now).phase)
    }

    @Test
    fun whenRunningPassedEndWhileAppWasClosedThenRelaunchShowsExtensionPrompt() {
        val state = machine.onLaunch(running(now - 1), LaunchSource.External, now)

        assertEquals(FocusPhase.ExtensionPrompt, state.phase)
    }

    @Test
    fun whenExtendedThenRunningWithExtensionUsed() {
        val prompt = FocusSessionState(FocusPhase.ExtensionPrompt)

        val state = machine.onExtendRequested(prompt, now)

        assertEquals(running(now + 5.minutes.inWholeMilliseconds, extensionUsed = true), state)
    }

    @Test
    fun whenExtensionAlreadyUsedThenExtendIgnored() {
        val prompt = FocusSessionState(FocusPhase.ExtensionPrompt, extensionUsed = true)

        assertEquals(prompt, machine.onExtendRequested(prompt, now))
    }

    @Test
    fun whenExtendRequestedOutsidePromptThenIgnored() {
        val current = running(now + 1.minutes.inWholeMilliseconds)

        assertEquals(current, machine.onExtendRequested(current, now))
    }

    @Test
    fun whenCloseRequestedThenExpired() {
        assertEquals(FocusPhase.Expired, machine.onCloseRequested().phase)
    }

    @Test
    fun whenRunningThenWarningFifteenSecondsBeforeEnd() {
        val endsAt = now + 1.minutes.inWholeMilliseconds

        assertEquals(endsAt - 15.seconds.inWholeMilliseconds, machine.warningAtMs(running(endsAt)))
    }

    @Test
    fun whenNotRunningThenNoWarning() {
        assertNull(machine.warningAtMs(FocusSessionState(FocusPhase.Grace, now)))
        assertNull(machine.warningAtMs(FocusSessionState(FocusPhase.AwaitingSelection)))
    }
}
