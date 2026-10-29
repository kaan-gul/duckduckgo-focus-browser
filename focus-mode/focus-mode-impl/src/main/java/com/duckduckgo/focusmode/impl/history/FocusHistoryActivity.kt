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

import android.os.Bundle
import android.text.format.DateUtils
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.duckduckgo.codegen.annotations.ContributeToActivityStarter
import com.duckduckgo.codegen.annotations.InjectWith
import com.duckduckgo.common.ui.DuckDuckGoActivity
import com.duckduckgo.common.ui.view.dialog.TextAlertDialogBuilder
import com.duckduckgo.common.ui.view.gone
import com.duckduckgo.common.ui.view.listitem.TwoLineListItem
import com.duckduckgo.common.ui.view.show
import com.duckduckgo.common.ui.viewbinding.viewBinding
import com.duckduckgo.common.utils.edgetoedge.EdgeToEdgeHandler
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.focusmode.impl.R
import com.duckduckgo.focusmode.impl.databinding.ActivityFocusHistoryBinding
import com.duckduckgo.focusmode.impl.ui.formatFocusDuration
import dev.zacsweers.metro.HasMemberInjections
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HasMemberInjections
@InjectWith(ActivityScope::class)
@ContributeToActivityStarter(FocusHistoryNoParams::class)
class FocusHistoryActivity : DuckDuckGoActivity() {

    @Inject
    lateinit var edgeToEdgeHandler: EdgeToEdgeHandler

    private val binding: ActivityFocusHistoryBinding by viewBinding()
    private val viewModel: FocusHistoryViewModel by bindViewModel()
    private val adapter = SessionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableTransparentEdgeToEdge()
        setContentView(binding.root)
        setupToolbar(binding.includeToolbar.toolbar)

        edgeToEdgeHandler.applyHorizontalSystemBarInsets(binding.root)
        edgeToEdgeHandler.applyStatusBarInsets(binding.includeToolbar.appBarLayout)
        edgeToEdgeHandler.applyNavigationBarInsets(binding.contentScrollView, drawBehindGestureNav = true)

        binding.sessionList.layoutManager = LinearLayoutManager(this)
        binding.sessionList.adapter = adapter
        binding.clearHistory.setClickListener { confirmClear() }

        viewModel.viewState
            .filterNotNull()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach(::render)
            .launchIn(lifecycleScope)
    }

    private fun render(state: FocusHistoryViewModel.ViewState) {
        val stats = state.stats
        binding.statToday.setSecondaryText(formatFocusDuration(stats.today))
        binding.statWeek.setSecondaryText(formatFocusDuration(stats.lastSevenDays))
        binding.statSessions.setSecondaryText(stats.sessionCount.toString())
        binding.statAverage.setSecondaryText(formatFocusDuration(stats.averageSession))

        adapter.submit(state.recent)
        if (state.isEmpty) {
            binding.emptyMessage.show()
            binding.recentHeader.gone()
            binding.clearHistory.gone()
        } else {
            binding.emptyMessage.gone()
            binding.recentHeader.show()
            binding.clearHistory.show()
        }
    }

    private fun confirmClear() {
        TextAlertDialogBuilder(this)
            .setTitle(R.string.focus_history_clear_title)
            .setMessage(R.string.focus_history_clear_message)
            .setPositiveButton(R.string.focus_history_clear_confirm)
            .setNegativeButton(R.string.focus_settings_cancel)
            .addEventListener(
                object : TextAlertDialogBuilder.EventListener() {
                    override fun onPositiveButtonClicked() = viewModel.onClearConfirmed()
                },
            )
            .show()
    }

    private inner class SessionAdapter : RecyclerView.Adapter<SessionAdapter.Holder>() {

        private var sessions: List<FocusSession> = emptyList()

        fun submit(items: List<FocusSession>) {
            sessions = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): Holder = Holder(
            TwoLineListItem(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            },
        )

        override fun getItemCount(): Int = sessions.size

        override fun onBindViewHolder(
            holder: Holder,
            position: Int,
        ) {
            val session = sessions[position]
            val started = DateUtils.formatDateTime(
                this@FocusHistoryActivity,
                session.startedAtMs,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
            )
            holder.item.setPrimaryText(formatFocusDuration(session.plannedMs.milliseconds))
            holder.item.setSecondaryText(
                if (session.extended) "$started · ${getString(R.string.focus_history_extended)}" else started,
            )
        }

        inner class Holder(val item: TwoLineListItem) : RecyclerView.ViewHolder(item)
    }
}
