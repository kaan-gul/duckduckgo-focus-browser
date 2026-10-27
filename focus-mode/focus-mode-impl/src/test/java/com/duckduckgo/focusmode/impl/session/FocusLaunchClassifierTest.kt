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

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusLaunchClassifierTest {

    @Test
    fun whenAppIconLaunchThenManual() {
        assertEquals(LaunchSource.Manual, FocusLaunchClassifier.classify("standard"))
    }

    @Test
    fun whenLaunchedFromLinkShortcutOrWidgetThenExternal() {
        listOf("url", "shortcut", "widget", "other").forEach {
            assertEquals(it, LaunchSource.External, FocusLaunchClassifier.classify(it))
        }
    }

    @Test
    fun whenSourceMissingThenManual() {
        assertEquals(LaunchSource.Manual, FocusLaunchClassifier.classify(null))
    }

    @Test
    fun whenSourceUnknownThenManual() {
        assertEquals(LaunchSource.Manual, FocusLaunchClassifier.classify("something_new"))
    }
}
