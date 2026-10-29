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
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class FocusDurationsTest {

    @Test
    fun whenInputIsAPositiveNumberThenItIsMinutes() {
        assertEquals(25.minutes, FocusDurations.parseCustomMinutes(" 25 "))
    }

    @Test
    fun whenInputIsAtTheBoundsThenAccepted() {
        assertEquals(1.minutes, FocusDurations.parseCustomMinutes("1"))
        assertEquals(FocusDurations.MAX_CUSTOM_MINUTES.minutes, FocusDurations.parseCustomMinutes("480"))
    }

    @Test
    fun whenInputIsOutOfRangeOrNotANumberThenRejected() {
        listOf("", "0", "-5", "481", "abc", "1.5").forEach {
            assertNull(it, FocusDurations.parseCustomMinutes(it))
        }
    }
}
