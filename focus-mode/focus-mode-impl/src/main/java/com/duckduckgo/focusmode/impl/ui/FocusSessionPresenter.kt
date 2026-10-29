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
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import com.duckduckgo.app.di.AppCoroutineScope
import com.duckduckgo.common.ui.view.button.DaxButton
import com.duckduckgo.common.ui.view.dialog.BackKeyListener
import com.duckduckgo.common.ui.view.dialog.DaxAlertDialog
import com.duckduckgo.common.ui.view.dialog.TextAlertDialogBuilder
import com.duckduckgo.common.utils.DispatcherProvider
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.focusmode.impl.R
import com.duckduckgo.focusmode.impl.databinding.SheetFocusDurationBinding
import com.duckduckgo.focusmode.impl.session.FocusDurations
import com.duckduckgo.focusmode.impl.session.FocusPhase
import com.duckduckgo.focusmode.impl.session.FocusSessionController
import com.duckduckgo.focusmode.impl.session.FocusSessionState
import com.duckduckgo.focusmode.impl.session.FocusSettings
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import com.duckduckgo.mobile.android.R as CommonR

/**
 * Turns a [FocusSessionState] into UI on the browser activity: the non-dismissible duration sheet, the extension prompt and the
 * final close. It never decides anything itself, it only reports what the user chose back to [FocusSessionController].
 */
@SingleIn(AppScope::class)
class FocusSessionPresenter @Inject constructor(
    private val controller: FocusSessionController,
    private val dispatchers: DispatcherProvider,
    @AppCoroutineScope private val appScope: CoroutineScope,
) {

    private enum class Kind { Picker, Extension }

    private var shownKind: Kind? = null
    private var shownDialog: Any? = null

    fun render(
        activity: Activity,
        state: FocusSessionState,
        settings: FocusSettings,
    ) {
        when (state.phase) {
            FocusPhase.AwaitingSelection -> show(Kind.Picker) { buildPicker(activity) }
            FocusPhase.ExtensionPrompt -> show(Kind.Extension) { buildExtensionPrompt(activity, settings) }
            FocusPhase.Expired -> {
                dismiss()
                activity.finishAffinity()
            }
            FocusPhase.Idle, FocusPhase.Grace, FocusPhase.Running -> dismiss()
        }
    }

    fun showWarning(activity: Activity) {
        Toast.makeText(activity, R.string.focus_warning_toast, Toast.LENGTH_LONG).show()
    }

    fun dismiss() {
        when (val dialog = shownDialog) {
            is BottomSheetDialog -> dialog.dismiss()
            is DaxAlertDialog -> dialog.dismiss()
        }
        shownDialog = null
        shownKind = null
    }

    private fun show(
        kind: Kind,
        build: () -> Any,
    ) {
        if (shownKind == kind) return
        dismiss()
        val dialog = build()
        shownKind = kind
        shownDialog = dialog
        when (dialog) {
            is BottomSheetDialog -> dialog.show()
            is DaxAlertDialog -> dialog.show()
        }
    }

    private fun buildPicker(activity: Activity): BottomSheetDialog {
        val binding = SheetFocusDurationBinding.inflate(LayoutInflater.from(activity))
        val dialog = BottomSheetDialog(activity, CommonR.style.Widget_DuckDuckGo_FireDialog)

        dialog.setContentView(binding.root)
        // Nothing but a choice may dismiss the sheet, otherwise the session would be unlimited.
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.behavior.apply {
            isDraggable = false
            isHideable = false
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnKeyListener(BackKeyListener { closeBrowser(activity) })

        PRESETS.forEach { (minutes, button) ->
            val duration = minutes.minutes
            button(binding).text = activity.formatFocusDuration(duration)
            button(binding).setOnClickListener { select(duration) }
        }
        bindCustomDuration(binding, activity)
        return dialog
    }

    private fun bindCustomDuration(
        binding: SheetFocusDurationBinding,
        activity: Activity,
    ) {
        binding.customInput.hint = activity.getString(R.string.focus_picker_custom_hint)
        binding.customInput.findEditText()?.apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(MAX_CUSTOM_DIGITS))
        }
        binding.presetCustom.setOnClickListener {
            binding.customGroup.visibility = View.VISIBLE
            binding.customInput.error = null
        }
        binding.customStart.setOnClickListener {
            val duration = FocusDurations.parseCustomMinutes(binding.customInput.text)
            if (duration == null) {
                binding.customInput.error = activity.getString(R.string.focus_picker_custom_error, FocusDurations.MAX_CUSTOM_MINUTES)
            } else {
                select(duration)
            }
        }
    }

    private fun buildExtensionPrompt(
        activity: Activity,
        settings: FocusSettings,
    ): DaxAlertDialog {
        val extension = activity.formatFocusDuration(settings.extensionDuration)
        return TextAlertDialogBuilder(activity)
            .setTitle(R.string.focus_extension_title)
            .setMessage(activity.getString(R.string.focus_extension_message, extension))
            .setPositiveButton(activity.getString(R.string.focus_extension_positive, extension))
            .setNegativeButton(R.string.focus_extension_negative)
            .setCancellable(false)
            .addEventListener(
                object : TextAlertDialogBuilder.EventListener() {
                    override fun onPositiveButtonClicked() {
                        appScope.launch(dispatchers.main()) { controller.extend() }
                    }

                    override fun onNegativeButtonClicked() = closeBrowser(activity)
                },
            )
            .build()
    }

    private fun select(duration: Duration) {
        appScope.launch(dispatchers.main()) { controller.selectDuration(duration) }
    }

    private fun closeBrowser(activity: Activity) {
        appScope.launch(dispatchers.main()) {
            controller.close()
            activity.finishAffinity()
        }
    }

    private fun View.findEditText(): EditText? = when (this) {
        is EditText -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findEditText() }
        else -> null
    }

    private companion object {
        const val MAX_CUSTOM_DIGITS = 3

        val PRESETS: List<Pair<Int, (SheetFocusDurationBinding) -> DaxButton>> = listOf(
            5 to { it.preset5 },
            10 to { it.preset10 },
            15 to { it.preset15 },
            30 to { it.preset30 },
            45 to { it.preset45 },
            60 to { it.preset60 },
            120 to { it.preset120 },
        )
    }
}
