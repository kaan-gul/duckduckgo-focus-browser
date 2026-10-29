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

import android.content.Context
import com.duckduckgo.focusmode.impl.R
import kotlin.time.Duration

private const val MINUTES_PER_HOUR = 60

fun Context.formatFocusDuration(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes.toInt()
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> getString(R.string.focus_duration_minutes, minutes)
        minutes == 0 -> getString(R.string.focus_duration_hours, hours)
        else -> getString(R.string.focus_duration_hours_minutes, hours, minutes)
    }
}
