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

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.duckduckgo.di.scopes.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class FocusSession(
    val startedAtMs: Long,
    val endedAtMs: Long,
    val plannedMs: Long,
    val extended: Boolean,
)

interface FocusHistoryRepository {
    fun sessions(): Flow<List<FocusSession>>
    suspend fun record(session: FocusSession)
    suspend fun clear()
}

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val plannedMs: Long,
    val extended: Boolean,
)

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMs DESC LIMIT $MAX_STORED_SESSIONS")
    fun sessions(): Flow<List<FocusSessionEntity>>

    @Insert
    suspend fun insert(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()

    // History is a convenience view, not an archive: capping it keeps the table small for a browser that runs for years.
    @Query("DELETE FROM focus_sessions WHERE id NOT IN (SELECT id FROM focus_sessions ORDER BY startedAtMs DESC LIMIT $MAX_STORED_SESSIONS)")
    suspend fun trim()

    companion object {
        const val MAX_STORED_SESSIONS = 500
    }
}

@Database(
    exportSchema = false,
    version = 1,
    entities = [FocusSessionEntity::class],
)
abstract class FocusHistoryDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealFocusHistoryRepository @Inject constructor(
    private val dao: FocusSessionDao,
) : FocusHistoryRepository {

    override fun sessions(): Flow<List<FocusSession>> = dao.sessions().map { rows -> rows.map { it.toSession() } }

    override suspend fun record(session: FocusSession) {
        dao.insert(session.toEntity())
        dao.trim()
    }

    override suspend fun clear() = dao.deleteAll()

    private fun FocusSessionEntity.toSession() = FocusSession(startedAtMs, endedAtMs, plannedMs, extended)

    private fun FocusSession.toEntity() = FocusSessionEntity(
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
        plannedMs = plannedMs,
        extended = extended,
    )
}
