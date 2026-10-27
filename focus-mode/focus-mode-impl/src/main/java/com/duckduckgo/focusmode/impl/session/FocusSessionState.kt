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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal enum class FocusPhase {
    Idle,
    Grace,
    AwaitingSelection,
    Running,
    ExtensionPrompt,
    Expired,
    ;

    val isActive: Boolean get() = this != Idle && this != Expired
}

internal data class FocusSessionState(
    val phase: FocusPhase = FocusPhase.Idle,
    val endsAtMs: Long = 0L,
    val extensionUsed: Boolean = false,
)

internal data class FocusSettings(
    val graceDuration: Duration = 3.minutes,
    val extensionEnabled: Boolean = true,
    val extensionDuration: Duration = 5.minutes,
    val warningLead: Duration = 15.seconds,
)
