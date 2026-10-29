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

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class FocusStatsTest {

    private val zone = ZoneId.of("UTC")
    private val now = LocalDateTime.of(2026, 10, 29, 15, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun session(
        daysAgo: Long,
        minutes: Int,
    ): FocusSession {
        val start = now - daysAgo * 24 * 60 * 60 * 1000
        val planned = minutes.minutes.inWholeMilliseconds
        return FocusSession(startedAtMs = start, endedAtMs = start + planned, plannedMs = planned, extended = false)
    }

    @Test
    fun whenNoSessionsThenEverythingIsZero() {
        assertEquals(FocusStats(Duration.ZERO, Duration.ZERO, 0, Duration.ZERO), FocusStats.compute(emptyList(), now, zone))
    }

    @Test
    fun whenSessionsSpanDaysThenTotalsAreBucketed() {
        val stats = FocusStats.compute(listOf(session(0, 30), session(0, 15), session(3, 60), session(10, 45)), now, zone)

        assertEquals(45.minutes, stats.today)
        assertEquals(105.minutes, stats.lastSevenDays)
        assertEquals(4, stats.sessionCount)
    }

    @Test
    fun whenSessionsExistThenAverageIsOverAllSessions() {
        val stats = FocusStats.compute(listOf(session(0, 10), session(1, 20)), now, zone)

        assertEquals(15.minutes, stats.averageSession)
    }

    @Test
    fun whenSessionStartedYesterdayJustBeforeMidnightThenNotCountedToday() {
        val yesterdayLate = LocalDateTime.of(2026, 10, 28, 23, 50).toInstant(ZoneOffset.UTC).toEpochMilli()
        val late = FocusSession(yesterdayLate, yesterdayLate + 600_000, 10.minutes.inWholeMilliseconds, extended = false)

        assertEquals(Duration.ZERO, FocusStats.compute(listOf(late), now, zone).today)
    }
}
