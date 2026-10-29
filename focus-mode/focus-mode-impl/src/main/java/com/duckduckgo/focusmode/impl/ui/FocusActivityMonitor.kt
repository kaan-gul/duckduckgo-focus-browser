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

package com.duckduckgo.focusmode.impl.ui

import android.app.Activity
import com.duckduckgo.app.di.AppCoroutineScope
import com.duckduckgo.browser.api.ActivityLifecycleCallbacks
import com.duckduckgo.common.utils.DispatcherProvider
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.focusmode.impl.session.FocusClock
import com.duckduckgo.focusmode.impl.session.FocusLaunchClassifier
import com.duckduckgo.focusmode.impl.session.FocusPhase
import com.duckduckgo.focusmode.impl.session.FocusSessionController
import com.duckduckgo.focusmode.impl.session.FocusSessionMachine
import com.duckduckgo.focusmode.impl.session.FocusSessionState
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the focus session from the browser's lifecycle, so the browser itself needs no focus-specific code. Timers only run while
 * the browser is in the foreground; end times are absolute, so anything that elapsed in the background is applied on the next resume.
 */
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class FocusActivityMonitor @Inject constructor(
    private val controller: FocusSessionController,
    private val presenter: FocusSessionPresenter,
    private val clock: FocusClock,
    private val dispatchers: DispatcherProvider,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : ActivityLifecycleCallbacks {

    private var sessionJob: Job? = null

    override fun onActivityResumed(activity: Activity) {
        if (!activity.isBrowserActivity()) return

        val launchSource = activity.intent?.getStringExtra(FocusLaunchClassifier.LAUNCH_SOURCE_EXTRA)
        sessionJob?.cancel()
        sessionJob = appScope.launch(dispatchers.main()) {
            controller.onBrowserResumed(launchSource)
            controller.state.filterNotNull().collectLatest { state ->
                presenter.render(activity, state, controller.currentSettings())
                awaitNextTransition(activity, state)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (!activity.isBrowserActivity()) return

        sessionJob?.cancel()
        sessionJob = null
        presenter.dismiss()
    }

    private suspend fun awaitNextTransition(
        activity: Activity,
        state: FocusSessionState,
    ) {
        when (state.phase) {
            FocusPhase.Grace -> waitUntil(state.endsAtMs)
            FocusPhase.Running -> {
                val warningAt = FocusSessionMachine(controller.currentSettings()).warningAtMs(state)
                if (warningAt != null && clock.nowMs() < warningAt) {
                    waitUntil(warningAt)
                    presenter.showWarning(activity)
                }
                waitUntil(state.endsAtMs)
            }
            else -> return
        }
        controller.tick()
    }

    private suspend fun waitUntil(timeMs: Long) {
        // The extra margin keeps a clock that reads a hair early from turning the tick into a no-op.
        delay((timeMs - clock.nowMs()).coerceAtLeast(0L) + TICK_MARGIN_MS)
    }

    // BrowserActivity lives in :app, which no module may depend on, hence the name check.
    private fun Activity.isBrowserActivity(): Boolean =
        generateSequence<Class<*>>(javaClass) { it.superclass }.any { it.name == BROWSER_ACTIVITY_CLASS }

    private companion object {
        const val BROWSER_ACTIVITY_CLASS = "com.duckduckgo.app.browser.BrowserActivity"
        const val TICK_MARGIN_MS = 50L
    }
}
