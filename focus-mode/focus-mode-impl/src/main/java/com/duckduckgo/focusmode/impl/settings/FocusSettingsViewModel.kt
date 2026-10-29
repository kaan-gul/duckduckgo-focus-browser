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

package com.duckduckgo.focusmode.impl.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckduckgo.codegen.annotations.ContributesViewModel
import com.duckduckgo.common.utils.DispatcherProvider
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.focusmode.impl.session.FocusSettings
import com.duckduckgo.focusmode.impl.store.FocusStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@ContributesViewModel(ActivityScope::class)
class FocusSettingsViewModel @Inject constructor(
    private val store: FocusStore,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val settings: StateFlow<FocusSettings> = store.settings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FocusSettings())

    fun onExtensionToggled(enabled: Boolean) {
        viewModelScope.launch(dispatchers.io()) { store.setExtensionEnabled(enabled) }
    }

    fun onExtensionMinutesSelected(minutes: Int) {
        viewModelScope.launch(dispatchers.io()) { store.setExtensionDuration(minutes.minutes) }
    }

    fun onGraceMinutesSelected(minutes: Int) {
        viewModelScope.launch(dispatchers.io()) { store.setGraceDuration(minutes.minutes) }
    }

    companion object {
        val EXTENSION_MINUTES = listOf(3, 5, 10, 15)
        val GRACE_MINUTES = listOf(1, 3, 5, 10)
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
