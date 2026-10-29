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

package com.duckduckgo.focusmode.impl.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckduckgo.codegen.annotations.ContributesViewModel
import com.duckduckgo.common.utils.DispatcherProvider
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.focusmode.impl.session.FocusClock
import com.duckduckgo.navigation.api.GlobalActivityStarter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/** Launches the focus session history screen. */
object FocusHistoryNoParams : GlobalActivityStarter.ActivityParams

@ContributesViewModel(ActivityScope::class)
class FocusHistoryViewModel @Inject constructor(
    private val repository: FocusHistoryRepository,
    private val clock: FocusClock,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    data class ViewState(
        val stats: FocusStats,
        val recent: List<FocusSession>,
    ) {
        val isEmpty: Boolean get() = recent.isEmpty()
    }

    val viewState: StateFlow<ViewState?> = repository.sessions()
        .map { sessions ->
            ViewState(
                stats = FocusStats.compute(sessions, clock.nowMs(), ZoneId.systemDefault()),
                recent = sessions.take(RECENT_LIMIT),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun onClearConfirmed() {
        viewModelScope.launch(dispatchers.io()) { repository.clear() }
    }

    private companion object {
        const val RECENT_LIMIT = 50
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
