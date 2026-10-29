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

/** What the history needs to know about the chosen session; the state machine itself only tracks the current end time. */
data class FocusSessionMeta(
    val startedAtMs: Long = 0L,
    val plannedMs: Long = 0L,
    val extended: Boolean = false,
) {
    val plannedEndMs: Long get() = startedAtMs + plannedMs
}

data class PersistedSession(
    val state: FocusSessionState = FocusSessionState(),
    val meta: FocusSessionMeta = FocusSessionMeta(),
)
