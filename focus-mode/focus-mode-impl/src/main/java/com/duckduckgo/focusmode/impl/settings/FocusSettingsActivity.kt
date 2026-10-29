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

import android.os.Bundle
import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.duckduckgo.codegen.annotations.ContributeToActivityStarter
import com.duckduckgo.codegen.annotations.InjectWith
import com.duckduckgo.common.ui.DuckDuckGoActivity
import com.duckduckgo.common.ui.view.dialog.RadioListAlertDialogBuilder
import com.duckduckgo.common.ui.view.dialog.RadioListOption
import com.duckduckgo.common.ui.viewbinding.viewBinding
import com.duckduckgo.common.utils.edgetoedge.EdgeToEdgeHandler
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.focusmode.impl.R
import com.duckduckgo.focusmode.impl.databinding.ActivityFocusSettingsBinding
import com.duckduckgo.focusmode.impl.history.FocusHistoryNoParams
import com.duckduckgo.focusmode.impl.session.FocusSettings
import com.duckduckgo.focusmode.impl.ui.formatFocusDuration
import com.duckduckgo.navigation.api.GlobalActivityStarter
import dev.zacsweers.metro.HasMemberInjections
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HasMemberInjections
@InjectWith(ActivityScope::class)
@ContributeToActivityStarter(FocusSettingsNoParams::class)
class FocusSettingsActivity : DuckDuckGoActivity() {

    @Inject
    lateinit var globalActivityStarter: GlobalActivityStarter

    @Inject
    lateinit var edgeToEdgeHandler: EdgeToEdgeHandler

    private val binding: ActivityFocusSettingsBinding by viewBinding()
    private val viewModel: FocusSettingsViewModel by bindViewModel()

    private val extensionToggleListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        viewModel.onExtensionToggled(isChecked)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableTransparentEdgeToEdge()
        setContentView(binding.root)
        setupToolbar(binding.includeToolbar.toolbar)

        edgeToEdgeHandler.applyHorizontalSystemBarInsets(binding.root)
        edgeToEdgeHandler.applyStatusBarInsets(binding.includeToolbar.appBarLayout)
        edgeToEdgeHandler.applyNavigationBarInsets(binding.contentScrollView, drawBehindGestureNav = true)

        binding.historyEntry.setClickListener { globalActivityStarter.start(this, FocusHistoryNoParams) }
        binding.extensionDuration.setClickListener {
            showDurationChoice(
                title = R.string.focus_settings_extension_duration,
                choices = FocusSettingsViewModel.EXTENSION_MINUTES,
                currentMinutes = viewModel.settings.value.extensionDuration.inWholeMinutes.toInt(),
                onChosen = viewModel::onExtensionMinutesSelected,
            )
        }
        binding.graceDuration.setClickListener {
            showDurationChoice(
                title = R.string.focus_settings_grace_duration,
                choices = FocusSettingsViewModel.GRACE_MINUTES,
                currentMinutes = viewModel.settings.value.graceDuration.inWholeMinutes.toInt(),
                onChosen = viewModel::onGraceMinutesSelected,
            )
        }

        viewModel.settings
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach(::render)
            .launchIn(lifecycleScope)
    }

    private fun render(settings: FocusSettings) {
        binding.extensionToggle.quietlySetIsChecked(settings.extensionEnabled, extensionToggleListener)
        binding.extensionDuration.isEnabled = settings.extensionEnabled
        binding.extensionDuration.setSecondaryText(formatFocusDuration(settings.extensionDuration))
        binding.graceDuration.setSecondaryText(formatFocusDuration(settings.graceDuration))
    }

    private fun showDurationChoice(
        @StringRes title: Int,
        choices: List<Int>,
        currentMinutes: Int,
        onChosen: (Int) -> Unit,
    ) {
        RadioListAlertDialogBuilder(this)
            .setTitle(title)
            .setOptions(choices.map { RadioListOption(optionLabel(it), isSelected = it == currentMinutes) })
            .setPositiveButton(R.string.focus_settings_apply)
            .setNegativeButton(R.string.focus_settings_cancel)
            .addEventListener(
                object : RadioListAlertDialogBuilder.EventListener() {
                    override fun onPositiveButtonClicked(selectedItem: Int) = onChosen(choices[selectedItem - 1])
                },
            )
            .show()
    }

    @StringRes
    private fun optionLabel(minutes: Int): Int = when (minutes) {
        1 -> R.string.focus_option_1_min
        3 -> R.string.focus_option_3_min
        5 -> R.string.focus_option_5_min
        10 -> R.string.focus_option_10_min
        else -> R.string.focus_option_15_min
    }
}
