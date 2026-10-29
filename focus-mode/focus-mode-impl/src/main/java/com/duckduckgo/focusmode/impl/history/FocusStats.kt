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

import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class FocusStats(
    val today: Duration,
    val lastSevenDays: Duration,
    val sessionCount: Int,
    val averageSession: Duration,
) {
    companion object {
        private const val WEEK_DAYS = 6L

        fun compute(
            sessions: List<FocusSession>,
            nowMs: Long,
            zone: ZoneId,
        ): FocusStats {
            val startOfToday = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
            val startOfWeek = Instant.ofEpochMilli(startOfToday).atZone(zone).minusDays(WEEK_DAYS).toInstant().toEpochMilli()

            fun total(since: Long) = sessions.filter { it.startedAtMs >= since }.sumOf { it.plannedMs }.milliseconds

            return FocusStats(
                today = total(startOfToday),
                lastSevenDays = total(startOfWeek),
                sessionCount = sessions.size,
                averageSession = if (sessions.isEmpty()) Duration.ZERO else (sessions.sumOf { it.plannedMs } / sessions.size).milliseconds,
            )
        }
    }
}
