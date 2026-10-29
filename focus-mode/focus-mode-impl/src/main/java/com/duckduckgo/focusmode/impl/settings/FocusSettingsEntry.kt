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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.duckduckgo.codegen.annotations.InjectWith
import com.duckduckgo.codegen.annotations.PriorityKey
import com.duckduckgo.common.ui.view.listitem.OneLineListItem
import com.duckduckgo.di.android.support.AndroidSupportInjection
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.di.scopes.ViewScope
import com.duckduckgo.focusmode.impl.R
import com.duckduckgo.navigation.api.GlobalActivityStarter
import com.duckduckgo.settings.api.OtherSettingsPlugin
import dev.zacsweers.metro.ContributesIntoMap
import javax.inject.Inject
import com.duckduckgo.mobile.android.R as CommonR

@ContributesIntoMap(ActivityScope::class)
@PriorityKey(FocusSettingsEntry.PRIORITY)
class FocusSettingsEntry @Inject constructor() : OtherSettingsPlugin {

    override fun getView(context: Context): View = FocusSettingsEntryView(context)

    companion object {
        const val PRIORITY = 90
    }
}

@InjectWith(ViewScope::class)
class FocusSettingsEntryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    @Inject
    lateinit var globalActivityStarter: GlobalActivityStarter

    private val listItem = OneLineListItem(context).apply {
        setLeadingIconResource(CommonR.drawable.history_color_24)
        setPrimaryText(context.getString(R.string.focus_settings_title))
    }

    init {
        orientation = VERTICAL
        addView(listItem)
    }

    override fun onAttachedToWindow() {
        AndroidSupportInjection.inject(this)
        super.onAttachedToWindow()
        listItem.setClickListener { globalActivityStarter.start(context, FocusSettingsNoParams) }
    }
}
