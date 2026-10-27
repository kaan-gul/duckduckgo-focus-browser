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

internal enum class LaunchSource {
    Manual,
    External,
}

internal object FocusLaunchClassifier {

    // :app owns this key and its values (BrowserActivity, LaunchSourceValues) and no module may depend on :app.
    const val LAUNCH_SOURCE_EXTRA = "LAUNCH_SOURCE_PIXEL_VALUE"

    private val EXTERNAL_VALUES = setOf("url", "shortcut", "widget", "other")

    // Unknown or missing values fall back to Manual so an unrecognised entry point can never skip the picker.
    fun classify(launchSourceValue: String?): LaunchSource =
        if (launchSourceValue in EXTERNAL_VALUES) LaunchSource.External else LaunchSource.Manual
}
