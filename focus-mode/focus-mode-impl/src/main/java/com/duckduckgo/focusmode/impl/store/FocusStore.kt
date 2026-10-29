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

package com.duckduckgo.focusmode.impl.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.focusmode.impl.di.FocusPreferences
import com.duckduckgo.focusmode.impl.session.FocusPhase
import com.duckduckgo.focusmode.impl.session.FocusSessionMeta
import com.duckduckgo.focusmode.impl.session.FocusSessionState
import com.duckduckgo.focusmode.impl.session.FocusSettings
import com.duckduckgo.focusmode.impl.session.PersistedSession
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

interface FocusStore {
    suspend fun loadSession(): PersistedSession
    suspend fun saveSession(session: PersistedSession)

    fun settings(): Flow<FocusSettings>
    suspend fun currentSettings(): FocusSettings
    suspend fun setExtensionEnabled(enabled: Boolean)
    suspend fun setExtensionDuration(duration: Duration)
    suspend fun setGraceDuration(duration: Duration)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealFocusStore @Inject constructor(
    @FocusPreferences private val dataStore: DataStore<Preferences>,
) : FocusStore {

    override suspend fun loadSession(): PersistedSession {
        val prefs = dataStore.data.first()
        // An unreadable phase (e.g. written by a newer build) must never block the browser, so it degrades to "no session".
        val phase = prefs[KEY_PHASE]?.let { name -> FocusPhase.entries.firstOrNull { it.name == name } } ?: FocusPhase.Idle
        return PersistedSession(
            state = FocusSessionState(
                phase = phase,
                endsAtMs = prefs[KEY_ENDS_AT] ?: 0L,
                extensionUsed = prefs[KEY_EXTENSION_USED] ?: false,
            ),
            meta = FocusSessionMeta(
                startedAtMs = prefs[KEY_STARTED_AT] ?: 0L,
                plannedMs = prefs[KEY_PLANNED] ?: 0L,
                extended = prefs[KEY_EXTENDED] ?: false,
            ),
        )
    }

    override suspend fun saveSession(session: PersistedSession) {
        dataStore.edit { prefs ->
            prefs[KEY_PHASE] = session.state.phase.name
            prefs[KEY_ENDS_AT] = session.state.endsAtMs
            prefs[KEY_EXTENSION_USED] = session.state.extensionUsed
            prefs[KEY_STARTED_AT] = session.meta.startedAtMs
            prefs[KEY_PLANNED] = session.meta.plannedMs
            prefs[KEY_EXTENDED] = session.meta.extended
        }
    }

    override fun settings(): Flow<FocusSettings> = dataStore.data.map { prefs ->
        val defaults = FocusSettings()
        FocusSettings(
            graceDuration = prefs[KEY_GRACE_MINUTES]?.minutes ?: defaults.graceDuration,
            extensionEnabled = prefs[KEY_EXTENSION_ENABLED] ?: defaults.extensionEnabled,
            extensionDuration = prefs[KEY_EXTENSION_MINUTES]?.minutes ?: defaults.extensionDuration,
        )
    }

    override suspend fun currentSettings(): FocusSettings = settings().first()

    override suspend fun setExtensionEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_EXTENSION_ENABLED] = enabled }
    }

    override suspend fun setExtensionDuration(duration: Duration) {
        dataStore.edit { it[KEY_EXTENSION_MINUTES] = duration.inWholeMinutes.toInt() }
    }

    override suspend fun setGraceDuration(duration: Duration) {
        dataStore.edit { it[KEY_GRACE_MINUTES] = duration.inWholeMinutes.toInt() }
    }

    private companion object {
        val KEY_PHASE = stringPreferencesKey("session_phase")
        val KEY_ENDS_AT = longPreferencesKey("session_ends_at_ms")
        val KEY_EXTENSION_USED = booleanPreferencesKey("session_extension_used")
        val KEY_STARTED_AT = longPreferencesKey("session_started_at_ms")
        val KEY_PLANNED = longPreferencesKey("session_planned_ms")
        val KEY_EXTENDED = booleanPreferencesKey("session_extended")
        val KEY_EXTENSION_ENABLED = booleanPreferencesKey("settings_extension_enabled")
        val KEY_EXTENSION_MINUTES = intPreferencesKey("settings_extension_minutes")
        val KEY_GRACE_MINUTES = intPreferencesKey("settings_grace_minutes")
    }
}
